#!/usr/bin/env python3
"""Project-owned code runner that executes each test in a disposable Docker container."""

from __future__ import annotations

import hmac
import json
import os
import re
import subprocess
import threading
import time
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any


MAX_REQUEST_BYTES = 128 * 1024
MAX_SOURCE_CHARS = 30_000
MAX_TESTS = 25
MAX_OUTPUT_CHARS = 1_000
PORT = int(os.getenv("RUNNER_PORT", "8090"))
API_KEY = os.getenv("RUNNER_API_KEY", "").strip()
MAX_PARALLEL = max(1, min(int(os.getenv("RUNNER_MAX_PARALLEL", "2")), 4))
SLOTS = threading.BoundedSemaphore(MAX_PARALLEL)

RUNTIME_IMAGES = {
    "JAVA": os.getenv("RUNNER_JAVA_IMAGE", "eclipse-temurin:17-jdk-alpine"),
    "PYTHON": os.getenv("RUNNER_PYTHON_IMAGE", "python:3.13-alpine"),
    "JAVASCRIPT": os.getenv("RUNNER_JAVASCRIPT_IMAGE", "node:24-alpine"),
    "CPP": os.getenv("RUNNER_CPP_IMAGE", "gcc:14"),
}

SUPPORTED_PROBLEMS = {
    "two-sum": ("array", {"JAVA": "twoSum", "PYTHON": "two_sum", "JAVASCRIPT": "twoSum", "CPP": "twoSum"}),
    "valid-parentheses": ("boolean", {"JAVA": "isValid", "PYTHON": "is_valid", "JAVASCRIPT": "isValid", "CPP": "isValid"}),
    "maximum-subarray": ("number", {"JAVA": "maxSubArray", "PYTHON": "max_sub_array", "JAVASCRIPT": "maxSubArray", "CPP": "maxSubArray"}),
    "binary-search": ("number", {"JAVA": "search", "PYTHON": "binary_search", "JAVASCRIPT": "binarySearch", "CPP": "binarySearch"}),
    "palindrome-number": ("boolean", {"JAVA": "isPalindrome", "PYTHON": "is_palindrome", "JAVASCRIPT": "isPalindrome", "CPP": "isPalindrome"}),
    "factorial": ("number", {"JAVA": "factorial", "PYTHON": "factorial", "JAVASCRIPT": "factorial", "CPP": "factorial"}),
    "fibonacci-number": ("number", {"JAVA": "fibonacci", "PYTHON": "fibonacci", "JAVASCRIPT": "fibonacci", "CPP": "fibonacci"}),
    "count-vowels": ("number", {"JAVA": "countVowels", "PYTHON": "count_vowels", "JAVASCRIPT": "countVowels", "CPP": "countVowels"}),
    "merge-sorted-arrays": ("array", {"JAVA": "mergeSorted", "PYTHON": "merge_sorted", "JAVASCRIPT": "mergeSorted", "CPP": "mergeSorted"}),
    "contains-duplicate": ("boolean", {"JAVA": "containsDuplicate", "PYTHON": "contains_duplicate", "JAVASCRIPT": "containsDuplicate", "CPP": "containsDuplicate"}),
}


class RequestError(ValueError):
    pass


def validate_request(payload: Any) -> dict[str, Any]:
    if not isinstance(payload, dict):
        raise RequestError("Request body must be a JSON object")
    slug = payload.get("problemSlug")
    language = payload.get("language")
    source = payload.get("sourceCode")
    tests = payload.get("testCases")
    time_limit = payload.get("timeLimitMs")
    memory_limit = payload.get("memoryLimitMb")
    if slug not in SUPPORTED_PROBLEMS:
        raise RequestError("Unsupported problem")
    if language not in RUNTIME_IMAGES:
        raise RequestError("Unsupported language")
    if not isinstance(source, str) or not source.strip() or len(source) > MAX_SOURCE_CHARS:
        raise RequestError("Source code is empty or too large")
    if not isinstance(tests, list) or not tests or len(tests) > MAX_TESTS:
        raise RequestError("Test cases are missing or exceed the limit")
    for test in tests:
        if not isinstance(test, dict) or "input" not in test or "expected" not in test:
            raise RequestError("Every test case requires input and expected fields")
    if not isinstance(time_limit, int) or not 100 <= time_limit <= 5_000:
        raise RequestError("timeLimitMs must be between 100 and 5000")
    if not isinstance(memory_limit, int) or not 64 <= memory_limit <= 512:
        raise RequestError("memoryLimitMb must be between 64 and 512")
    return payload


def json_value(value: Any) -> str:
    return json.dumps(value, ensure_ascii=True, separators=(",", ":"))


def java_int_array(values: Any) -> str:
    if not isinstance(values, list) or any(not isinstance(value, int) for value in values):
        raise RequestError("Expected an integer array")
    return "new int[]{" + ",".join(str(value) for value in values) + "}"


def cpp_int_vector(values: Any) -> str:
    if not isinstance(values, list) or any(not isinstance(value, int) for value in values):
        raise RequestError("Expected an integer array")
    return "vector<int>{" + ",".join(str(value) for value in values) + "}"


def invocation(slug: str, language: str, inputs: Any) -> tuple[str, str]:
    if not isinstance(inputs, dict):
        raise RequestError("Test input must be an object")
    result_kind, names = SUPPORTED_PROBLEMS[slug]
    function = names[language]

    if slug in {"two-sum", "binary-search"}:
        nums, target = inputs.get("nums"), inputs.get("target")
        if not isinstance(target, int):
            raise RequestError("target must be an integer")
        if language == "JAVA":
            return "", f"new Solution().{function}({java_int_array(nums)}, {target})"
        if language == "CPP":
            return f"auto nums = {cpp_int_vector(nums)};", f"{function}(nums, {target})"
        return "", f"{function}({json_value(nums)}, {target})"
    if slug in {"maximum-subarray", "contains-duplicate"}:
        nums = inputs.get("nums")
        if language == "JAVA":
            return "", f"new Solution().{function}({java_int_array(nums)})"
        if language == "CPP":
            return f"auto nums = {cpp_int_vector(nums)};", f"{function}(nums)"
        return "", f"{function}({json_value(nums)})"
    if slug == "merge-sorted-arrays":
        first, second = inputs.get("first"), inputs.get("second")
        if language == "JAVA":
            return "", f"new Solution().{function}({java_int_array(first)}, {java_int_array(second)})"
        if language == "CPP":
            return f"auto first = {cpp_int_vector(first)}; auto second = {cpp_int_vector(second)};", f"{function}(first, second)"
        return "", f"{function}({json_value(first)}, {json_value(second)})"
    if slug in {"valid-parentheses", "count-vowels"}:
        value = inputs.get("value")
        if not isinstance(value, str):
            raise RequestError("value must be a string")
        call = f"{function}({json_value(value)})"
        return "", f"new Solution().{call}" if language == "JAVA" else call
    if slug == "palindrome-number":
        value = inputs.get("value")
        if not isinstance(value, int):
            raise RequestError("value must be an integer")
        call = f"{function}({value})"
        return "", f"new Solution().{call}" if language == "JAVA" else call
    if slug in {"factorial", "fibonacci-number"}:
        value = inputs.get("n")
        if not isinstance(value, int):
            raise RequestError("n must be an integer")
        call = f"{function}({value})"
        return "", f"new Solution().{call}" if language == "JAVA" else call
    raise RequestError("Unsupported problem")


def build_source(slug: str, language: str, source: str, inputs: Any) -> str:
    result_kind = SUPPORTED_PROBLEMS[slug][0]
    setup, call = invocation(slug, language, inputs)
    if language == "JAVA":
        clean = re.sub(r"\bpublic\s+class\s+Solution\b", "class Solution", source)
        output = "System.out.println(java.util.Arrays.toString(result));" if result_kind == "array" else "System.out.println(result);"
        return f"import java.util.*;\n{clean}\nclass Main {{ public static void main(String[] args) {{ {setup} var result = {call}; {output} }} }}\n"
    if language == "PYTHON":
        return f"{source}\n\nimport json\n{setup}\n_result = {call}\nprint(json.dumps(_result, separators=(',', ':')))\n"
    if language == "JAVASCRIPT":
        return f"{source}\n\n{setup}\nconst __result = {call};\nconsole.log(JSON.stringify(__result));\n"
    if language == "CPP":
        if result_kind == "array":
            output = "cout << '['; for (size_t i=0;i<result.size();++i) { if(i) cout << ','; cout << result[i]; } cout << ']';"
        else:
            output = "cout << boolalpha << result;"
        return f"#include <bits/stdc++.h>\nusing namespace std;\n{source}\nint main() {{ {setup} auto result = {call}; {output} return 0; }}\n"
    raise RequestError("Unsupported language")


def docker_command(language: str, time_limit_ms: int, memory_limit_mb: int) -> list[str]:
    seconds = max(1, (time_limit_ms + 999) // 1000)
    common = [
        "docker", "run", "--rm", "-i", "--network", "none", "--read-only",
        "--tmpfs", "/tmp:rw,exec,nosuid,nodev,size=64m,mode=1777", "--cap-drop", "ALL",
        "--security-opt", "no-new-privileges", "--pids-limit", "64",
        "--memory", f"{memory_limit_mb}m", "--memory-swap", f"{memory_limit_mb}m",
        "--cpus", "1.0", "--user", "65534:65534", "--workdir", "/tmp",
        RUNTIME_IMAGES[language], "sh", "-lc",
    ]
    if language == "JAVA":
        heap = max(32, memory_limit_mb * 3 // 5)
        script = f"cat > Main.java; javac Main.java >/tmp/out 2>/tmp/err || {{ head -c 1000 /tmp/err >&2; exit 100; }}; timeout -s KILL {seconds} java -Xmx{heap}m -cp /tmp Main >/tmp/out 2>/tmp/err; rc=$?; head -c 65536 /tmp/out; head -c 1000 /tmp/err >&2; exit $rc"
    elif language == "CPP":
        script = f"cat > main.cpp; g++ -std=c++20 -O2 -pipe main.cpp -o main >/tmp/out 2>/tmp/err || {{ head -c 1000 /tmp/err >&2; exit 100; }}; timeout -s KILL {seconds} ./main >/tmp/out 2>/tmp/err; rc=$?; head -c 65536 /tmp/out; head -c 1000 /tmp/err >&2; exit $rc"
    elif language == "PYTHON":
        script = f"cat > main.py; timeout -s KILL {seconds} python3 -I main.py >/tmp/out 2>/tmp/err; rc=$?; head -c 65536 /tmp/out; head -c 1000 /tmp/err >&2; exit $rc"
    else:
        script = f"cat > main.js; timeout -s KILL {seconds} node --disable-proto=delete main.js >/tmp/out 2>/tmp/err; rc=$?; head -c 65536 /tmp/out; head -c 1000 /tmp/err >&2; exit $rc"
    return common + [script]


def final_line(stdout: str) -> Any:
    lines = [line.strip() for line in stdout.splitlines() if line.strip()]
    if not lines:
        raise ValueError("Program produced no result")
    return json.loads(lines[-1])


def values_equal(actual: Any, expected: Any) -> bool:
    """Compare JSON values without treating booleans as integers."""
    if isinstance(actual, bool) or isinstance(expected, bool):
        return type(actual) is type(expected) and actual == expected
    if isinstance(actual, (int, float)) and isinstance(expected, (int, float)):
        return actual == expected
    return type(actual) is type(expected) and actual == expected


def result(verdict: str, passed: int, total: int, elapsed_ms: int, message: str) -> dict[str, Any]:
    score = round((passed * 100.0 / total) if total else 0.0, 2)
    return {
        "verdict": verdict,
        "passedTestCases": passed,
        "totalTestCases": total,
        "executionTimeMs": elapsed_ms,
        "memoryUsedKb": None,
        "score": score,
        "message": message[:MAX_OUTPUT_CHARS],
    }


def execute(payload: dict[str, Any]) -> dict[str, Any]:
    language = payload["language"]
    tests = payload["testCases"]
    passed = 0
    started = time.monotonic()
    for index, test in enumerate(tests, start=1):
        program = build_source(payload["problemSlug"], language, payload["sourceCode"], test["input"])
        try:
            completed = subprocess.run(
                docker_command(language, payload["timeLimitMs"], payload["memoryLimitMb"]),
                input=program, text=True, capture_output=True,
                timeout=max(20, payload["timeLimitMs"] / 1000 + 15), check=False,
            )
        except subprocess.TimeoutExpired:
            elapsed = int((time.monotonic() - started) * 1000)
            return result("TIME_LIMIT_EXCEEDED", passed, len(tests), elapsed, f"Test {index} exceeded the time limit")
        elapsed = int((time.monotonic() - started) * 1000)
        if completed.returncode in {124, 137}:
            return result("TIME_LIMIT_EXCEEDED", passed, len(tests), elapsed, f"Test {index} exceeded the time limit")
        if completed.returncode == 100 or (language in {"PYTHON", "JAVASCRIPT"} and "SyntaxError" in completed.stderr):
            return result("COMPILE_ERROR", passed, len(tests), elapsed, completed.stderr or "Compilation failed")
        if completed.returncode != 0:
            return result("RUNTIME_ERROR", passed, len(tests), elapsed, completed.stderr or f"Test {index} failed at runtime")
        try:
            actual = final_line(completed.stdout)
        except (ValueError, json.JSONDecodeError):
            return result("RUNTIME_ERROR", passed, len(tests), elapsed, f"Test {index} did not produce a valid result")
        if not values_equal(actual, test["expected"]):
            return result("WRONG_ANSWER", passed, len(tests), elapsed, f"Test {index} failed")
        passed += 1
    elapsed = int((time.monotonic() - started) * 1000)
    return result("ACCEPTED", passed, len(tests), elapsed, "All tests passed")


class Handler(BaseHTTPRequestHandler):
    server_version = "InterviewPilotRunner/1.0"

    def do_GET(self) -> None:
        if self.path == "/health":
            self.send_json(HTTPStatus.OK, {"status": "UP", "languages": sorted(RUNTIME_IMAGES)})
        else:
            self.send_json(HTTPStatus.NOT_FOUND, {"error": "Not found"})

    def do_POST(self) -> None:
        if self.path != "/v1/execute":
            self.send_json(HTTPStatus.NOT_FOUND, {"error": "Not found"})
            return
        if API_KEY and not hmac.compare_digest(self.headers.get("Authorization", ""), f"Bearer {API_KEY}"):
            self.send_json(HTTPStatus.UNAUTHORIZED, {"error": "Unauthorized"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_REQUEST_BYTES:
                raise RequestError("Request body is empty or too large")
            payload = validate_request(json.loads(self.rfile.read(length)))
        except (ValueError, json.JSONDecodeError, UnicodeDecodeError) as exc:
            self.send_json(HTTPStatus.BAD_REQUEST, {"error": str(exc)})
            return
        if not SLOTS.acquire(blocking=False):
            self.send_json(HTTPStatus.TOO_MANY_REQUESTS, {"error": "Runner is busy"})
            return
        try:
            self.send_json(HTTPStatus.OK, execute(payload))
        except RequestError as exc:
            self.send_json(HTTPStatus.UNPROCESSABLE_ENTITY, {"error": str(exc)})
        except Exception:
            self.send_json(HTTPStatus.INTERNAL_SERVER_ERROR, {"error": "Execution failed"})
        finally:
            SLOTS.release()

    def send_json(self, status: HTTPStatus, payload: dict[str, Any]) -> None:
        body = json.dumps(payload, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt: str, *args: Any) -> None:
        print(f"runner {self.address_string()} {fmt % args}", flush=True)


def prewarm_images() -> None:
    """Fail startup early when Docker is unavailable and pull missing runtimes once."""
    if len(API_KEY) < 16:
        raise RuntimeError("RUNNER_API_KEY must contain at least 16 characters")
    subprocess.run(
        ["docker", "version"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        timeout=15,
        check=True,
    )
    for image in dict.fromkeys(RUNTIME_IMAGES.values()):
        inspected = subprocess.run(
            ["docker", "image", "inspect", image],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            timeout=15,
            check=False,
        )
        if inspected.returncode != 0:
            print(f"runner pulling runtime image {image}", flush=True)
            subprocess.run(["docker", "pull", image], timeout=900, check=True)


if __name__ == "__main__":
    prewarm_images()
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
