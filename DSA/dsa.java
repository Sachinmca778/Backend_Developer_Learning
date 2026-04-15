// Longest Substring Without Repeating Characters

s = "abcabcbb"
Answer = 3  ("abc")

💡 Step by Step Logic
1️⃣ Ek HashSet lo
👉 unique characters store karega

2️⃣ 2 pointer lo
left = 0
right = 0

3️⃣ Right ko aage badhao (loop)
Har step pe:

👉 Case 1: character NEW hai
add karo set me
window badao
👉 Case 2: character DUPLICATE hai
left ko aage badhao
aur characters remove karo
jab tak duplicate hat na jaaye

4️⃣ Har step pe length update karo
length = right - left + 1

import java.util.*;

public class LongestSubstring {
    public static int lengthOfLongestSubstring(String s) {
        
        HashSet<Character> set = new HashSet<>();
        
        int left = 0;
        int maxLength = 0;

        for (int right = 0; right < s.length(); right++) {
            
            char ch = s.charAt(right);

            while (set.contains(ch)) {
                set.remove(s.charAt(left));
                left++;
            }

            set.add(ch);

            int len = right - left + 1;
            if (len > maxLength) {
                maxLength = len;
            }
        }

        return maxLength;
    }
}

//Two Sum
💡 Step by Step Logic
1️⃣ Ek HashMap lo
👉 store karega:
number → index

2️⃣ Loop chalao array pe
for (int i = 0; i < nums.length; i++)

3️⃣ Har step pe:
👉 Step A: complement nikalo
complement = target - nums[i]

👉 Step B: check karo
kya complement map me hai?

✔️ Agar hai:
👉 answer mil gaya
❌ Agar nahi:
👉 current number map me daal do

import java.util.*;

public class TwoSum {
    public static int[] twoSum(int[] nums, int target) {
        
        HashMap<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            
            int complement = target - nums[i];

            if (map.containsKey(complement)) {
                return new int[]{map.get(complement), i};
            }

            map.put(nums[i], i);
        }

        return new int[]{-1, -1};
    }
}

// Maximum Subarray Sum
💡 Step by Step Logic (Kadane’s Algorithm)
1️⃣ 2 variables lo
currentSum =0;
maxSum = arr[0];

2️⃣ Loop chalao array pe
Har step pe:

👉 Step A:
currentSum += nums[i]

👉 Step B:
maxSum = max(maxSum, currentSum)

👉 Step C:
agar currentSum < 0 → reset to 0

public class MaximumSubarray {
    public static int maxSubArray(int[] nums) {
        
        int currentSum = 0;
        int maxSum = nums[0];

        for (int i = 0; i < nums.length; i++) {
            
            currentSum += nums[i];

            if (currentSum > maxSum) {
                maxSum = currentSum;
            }

            if (currentSum < 0) {
                currentSum = 0;
            }
        }

        return maxSum;
    }

    public static void main(String[] args) {
        int[] nums = {-2,1,-3,4,-1,2,1,-5,4};
        System.out.println(maxSubArray(nums));
    }
}


// Merge Intervals (Medium)
💡 Step by Step Logic
1️⃣ Sort intervals
👉 start ke basis pe

2️⃣ Result list lo
👉 first interval add karo

3️⃣ Loop chalao
Har interval ke liye:

👉 Case 1: Overlap hai
current.start <= last.end
👉 merge karo:
last.end = max(last.end, current.end)

👉 Case 2: Overlap nahi hai
👉 directly add kar do result me



import java.util.*;

public class MergeIntervals {
    public static int[][] merge(int[][] intervals) {
        
        // Step 1: sort based on start
        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);

        List<int[]> result = new ArrayList<>();

        // Step 2: add first interval
        result.add(intervals[0]);

        // Step 3: loop
        for (int i = 1; i < intervals.length; i++) {
            
            int[] last = result.get(result.size() - 1);
            int[] current = intervals[i];

            // overlap
            if (current[0] <= last[1]) {
                last[1] = Math.max(last[1], current[1]);
            } 
            else {
                result.add(current);
            }
        }

        return result.toArray(new int[result.size()][]);
    }

    public static void main(String[] args) {
        int[][] intervals = {{1,3},{2,6},{8,10},{15,18}};
        
        int[][] res = merge(intervals);

        for (int i = 0; i < res.length; i++) {
            System.out.println(res[i][0] + " " + res[i][1]);
        }
    }
}


// Valid Parentheses (Easy)
💡 Step by Step Logic
1️⃣ Stack lo
👉 brackets store karne ke liye

2️⃣ Loop chalao string pe
👉 Case 1: Opening bracket
(  {  [

👉 stack me push karo

👉 Case 2: Closing bracket
)  }  ]

👉 check karo:
stack empty hai → ❌ invalid
top element matching hai → pop
match nahi → ❌ invalid

3️⃣ End me check
👉 stack empty hona chahiye

import java.util.*;

public class ValidParentheses {
    public static boolean isValid(String s) {
        
        Stack<Character> stack = new Stack<>();

        for (int i = 0; i < s.length(); i++) {
            
            char ch = s.charAt(i);

            // opening brackets
            if (ch == '(' || ch == '{' || ch == '[') {
                stack.push(ch);
            } 
            else {
                // if stack empty → invalid
                if (stack.isEmpty()) {
                    return false;
                }

                char top = stack.peek();

                // check matching
                if ((ch == ')' && top == '(') ||
                    (ch == '}' && top == '{') ||
                    (ch == ']' && top == '[')) {
                    
                    stack.pop();
                } 
                else {
                    return false;
                }
            }
        }

        // final check
        return stack.isEmpty();
    }

    public static void main(String[] args) {
        System.out.println(isValid("()[]{}"));
    }
}













