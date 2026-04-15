import java.util.HashSet;
public class Problem {

    public static void main (String[] args) {
        String s = "abcaabced";
        int result = longestsubstringwithoutrepeat(s);
        System.out.println(result);

    }

    public static int longestsubstringwithoutrepeat ( String s) {
        HashSet<Character> set = new HashSet<Character>();

        int left =0;
        int maxLength =0;
        for(int right=0; right < s.length() ; right++) {
            char  ch = s.charAt(right);
            while (set.contains(ch)) {
                set.remove(s.charAt(left));
                left++;
            }
            set.add(ch);
            int length = right - left + 1;
            if(length > maxLength) {
                maxLength = length;
            }
           
        }
        return maxLength;
    }
}