INSERT INTO coding_problems
    (title, slug, description, difficulty, starter_code, test_cases, tags, active)
VALUES
    (
        'Binary Search',
        'binary-search',
        'Given a sorted integer array and a target, return its index or -1 when the target is absent.',
        'EASY',
        '{"JAVA":"class Solution { public int search(int[] nums, int target) { } }","PYTHON":"def binary_search(nums, target):\\n    pass","JAVASCRIPT":"function binarySearch(nums, target) { }","CPP":"int binarySearch(vector<int>& nums, int target) { }"}',
        '[{"input":{"nums":[-1,0,3,5,9,12],"target":9},"expected":4},{"input":{"nums":[-1,0,3,5,9,12],"target":2},"expected":-1},{"input":{"nums":[5],"target":5},"expected":0}]',
        '["arrays","binary-search"]',
        TRUE
    ),
    (
        'Palindrome Number',
        'palindrome-number',
        'Return true when an integer reads the same forwards and backwards. Negative numbers are not palindromes.',
        'EASY',
        '{"JAVA":"class Solution { public boolean isPalindrome(int value) { } }","PYTHON":"def is_palindrome(value):\\n    pass","JAVASCRIPT":"function isPalindrome(value) { }","CPP":"bool isPalindrome(int value) { }"}',
        '[{"input":{"value":121},"expected":true},{"input":{"value":-121},"expected":false},{"input":{"value":10},"expected":false},{"input":{"value":0},"expected":true}]',
        '["math","strings"]',
        TRUE
    ),
    (
        'Factorial',
        'factorial',
        'Return n factorial for an integer from 0 through 20.',
        'EASY',
        '{"JAVA":"class Solution { public long factorial(int n) { } }","PYTHON":"def factorial(n):\\n    pass","JAVASCRIPT":"function factorial(n) { }","CPP":"long long factorial(int n) { }"}',
        '[{"input":{"n":0},"expected":1},{"input":{"n":1},"expected":1},{"input":{"n":5},"expected":120},{"input":{"n":10},"expected":3628800}]',
        '["math","recursion"]',
        TRUE
    ),
    (
        'Fibonacci Number',
        'fibonacci-number',
        'Return the nth Fibonacci number where F(0)=0 and F(1)=1.',
        'EASY',
        '{"JAVA":"class Solution { public int fibonacci(int n) { } }","PYTHON":"def fibonacci(n):\\n    pass","JAVASCRIPT":"function fibonacci(n) { }","CPP":"int fibonacci(int n) { }"}',
        '[{"input":{"n":0},"expected":0},{"input":{"n":1},"expected":1},{"input":{"n":7},"expected":13},{"input":{"n":10},"expected":55}]',
        '["dynamic-programming","recursion"]',
        TRUE
    ),
    (
        'Count Vowels',
        'count-vowels',
        'Count the English vowels in a string without treating uppercase and lowercase differently.',
        'EASY',
        '{"JAVA":"class Solution { public int countVowels(String value) { } }","PYTHON":"def count_vowels(value):\\n    pass","JAVASCRIPT":"function countVowels(value) { }","CPP":"int countVowels(string value) { }"}',
        '[{"input":{"value":"Interview"},"expected":4},{"input":{"value":"rhythm"},"expected":0},{"input":{"value":"AEIOU"},"expected":5},{"input":{"value":""},"expected":0}]',
        '["strings","iteration"]',
        TRUE
    ),
    (
        'Merge Sorted Arrays',
        'merge-sorted-arrays',
        'Merge two sorted integer arrays into one sorted result.',
        'EASY',
        '{"JAVA":"class Solution { public int[] mergeSorted(int[] first, int[] second) { } }","PYTHON":"def merge_sorted(first, second):\\n    pass","JAVASCRIPT":"function mergeSorted(first, second) { }","CPP":"vector<int> mergeSorted(vector<int>& first, vector<int>& second) { }"}',
        '[{"input":{"first":[1,3,5],"second":[2,4,6]},"expected":[1,2,3,4,5,6]},{"input":{"first":[],"second":[1]},"expected":[1]},{"input":{"first":[1,2],"second":[3,4]},"expected":[1,2,3,4]}]',
        '["arrays","two-pointers"]',
        TRUE
    ),
    (
        'Contains Duplicate',
        'contains-duplicate',
        'Return true when any integer appears at least twice in the array.',
        'EASY',
        '{"JAVA":"class Solution { public boolean containsDuplicate(int[] nums) { } }","PYTHON":"def contains_duplicate(nums):\\n    pass","JAVASCRIPT":"function containsDuplicate(nums) { }","CPP":"bool containsDuplicate(vector<int>& nums) { }"}',
        '[{"input":{"nums":[1,2,3,1]},"expected":true},{"input":{"nums":[1,2,3,4]},"expected":false},{"input":{"nums":[1,1,1,3,3,4,3,2,4,2]},"expected":true}]',
        '["arrays","hash-set"]',
        TRUE
    );
