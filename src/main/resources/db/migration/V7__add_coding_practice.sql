ALTER TABLE coding_submissions
    ADD COLUMN result_message VARCHAR(1000) NULL AFTER score,
    ADD CONSTRAINT chk_submissions_language
        CHECK (language IN ('JAVA', 'PYTHON', 'JAVASCRIPT', 'CPP')),
    ADD CONSTRAINT chk_submissions_test_counts
        CHECK (passed_test_cases >= 0 AND total_test_cases >= 0 AND passed_test_cases <= total_test_cases),
    ADD CONSTRAINT chk_submissions_execution_time
        CHECK (execution_time_ms IS NULL OR execution_time_ms >= 0),
    ADD CONSTRAINT chk_submissions_memory
        CHECK (memory_used_kb IS NULL OR memory_used_kb >= 0),
    ADD CONSTRAINT chk_submissions_score
        CHECK (score IS NULL OR (score >= 0 AND score <= 100));

INSERT INTO coding_problems
    (title, slug, description, difficulty, starter_code, test_cases, tags, active)
VALUES
    (
        'Two Sum',
        'two-sum',
        'Given an integer array and a target, return the indices of two distinct values whose sum equals the target. Exactly one valid answer exists.',
        'EASY',
        '{"JAVA":"class Solution { public int[] twoSum(int[] nums, int target) { } }","PYTHON":"def two_sum(nums, target):\\n    pass","JAVASCRIPT":"function twoSum(nums, target) { }","CPP":"vector<int> twoSum(vector<int>& nums, int target) { }"}',
        '[{"input":{"nums":[2,7,11,15],"target":9},"expected":[0,1]},{"input":{"nums":[3,2,4],"target":6},"expected":[1,2]},{"input":{"nums":[3,3],"target":6},"expected":[0,1]}]',
        '["arrays","hash-map"]',
        TRUE
    ),
    (
        'Valid Parentheses',
        'valid-parentheses',
        'Given a string containing only parentheses and brackets, return whether every opening character is closed in the correct order.',
        'EASY',
        '{"JAVA":"class Solution { public boolean isValid(String value) { } }","PYTHON":"def is_valid(value):\\n    pass","JAVASCRIPT":"function isValid(value) { }","CPP":"bool isValid(string value) { }"}',
        '[{"input":{"value":"()"},"expected":true},{"input":{"value":"()[]{}"},"expected":true},{"input":{"value":"([)]"},"expected":false},{"input":{"value":"{[]}"},"expected":true}]',
        '["strings","stack"]',
        TRUE
    ),
    (
        'Maximum Subarray',
        'maximum-subarray',
        'Given an integer array, return the largest possible sum of a non-empty contiguous subarray.',
        'MEDIUM',
        '{"JAVA":"class Solution { public int maxSubArray(int[] nums) { } }","PYTHON":"def max_sub_array(nums):\\n    pass","JAVASCRIPT":"function maxSubArray(nums) { }","CPP":"int maxSubArray(vector<int>& nums) { }"}',
        '[{"input":{"nums":[-2,1,-3,4,-1,2,1,-5,4]},"expected":6},{"input":{"nums":[1]},"expected":1},{"input":{"nums":[5,4,-1,7,8]},"expected":23}]',
        '["arrays","dynamic-programming"]',
        TRUE
    );
