import importlib.util
import pathlib
import unittest


SPEC = importlib.util.spec_from_file_location("runner", pathlib.Path(__file__).with_name("runner.py"))
runner = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(runner)


class RunnerTest(unittest.TestCase):
    def test_builds_all_language_harnesses(self):
        cases = {
            "JAVA": "class Solution { int[] twoSum(int[] nums, int target) { return new int[]{0,1}; } }",
            "PYTHON": "def two_sum(nums, target):\n    return [0, 1]",
            "JAVASCRIPT": "function twoSum(nums, target) { return [0, 1]; }",
            "CPP": "vector<int> twoSum(vector<int>& nums, int target) { return {0,1}; }",
        }
        for language, source in cases.items():
            built = runner.build_source("two-sum", language, source, {"nums": [2, 7], "target": 9})
            self.assertIn("two", built.lower())
            self.assertIn("result", built.lower())

    def test_rejects_unknown_problem_and_oversized_limits(self):
        payload = {
            "problemSlug": "unknown",
            "language": "JAVA",
            "sourceCode": "class Solution {}",
            "testCases": [{"input": {}, "expected": 1}],
            "timeLimitMs": 2000,
            "memoryLimitMb": 256,
        }
        with self.assertRaises(runner.RequestError):
            runner.validate_request(payload)
        payload["problemSlug"] = "factorial"
        payload["timeLimitMs"] = 6000
        with self.assertRaises(runner.RequestError):
            runner.validate_request(payload)

    def test_final_line_uses_json_result(self):
        self.assertEqual([0, 1], runner.final_line("debug\n[0,1]\n"))
        self.assertTrue(runner.final_line("true\n"))

    def test_result_comparison_keeps_boolean_type(self):
        self.assertFalse(runner.values_equal(True, 1))
        self.assertTrue(runner.values_equal(1, 1.0))
        self.assertTrue(runner.values_equal([1, 2], [1, 2]))


if __name__ == "__main__":
    unittest.main()
