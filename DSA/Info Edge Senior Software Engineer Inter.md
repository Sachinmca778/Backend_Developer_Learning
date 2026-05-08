Info Edge Senior Software Engineer Interview Experiences
11 interviews found
Senior Software Engineer Interview Questions & Answers
user imageAnonymous
posted on 7 Jan 2026

Interview experience
4
Good
Difficulty level
Moderate
Process Duration
2-4 weeks
Result
SelectedSelected
I appeared for an interview in Dec 2025, where I was asked the following questions.

Q1. Redis vs local cache
Ans. Redis is a distributed in-memory data store, while local cache is a temporary storage within an application for quick data access.
Redis supports persistence, allowing data to survive server restarts, while local cache is typically volatile.

Redis can be accessed by multiple applications or services, making it suitable for distributed systems; local cache is limited to a single application instance.

Redis offers advanced data structures like lists, sets, and hashes, while local cache usually stores simple key-value pairs.

Example: Using Redis for session storage in a web application allows multiple servers to share session data, while local cache can speed up repeated database queries within a single server.

Answered by AI
Add your answer
Q2. How do you bug high latency in production?
Ans. Identify and resolve high latency issues in production through monitoring, analysis, and optimization techniques.
Monitor application performance using tools like New Relic or Datadog to identify latency hotspots.

Analyze logs for slow queries or errors; for example, using ELK stack (Elasticsearch, Logstash, Kibana).

Profile the application to find bottlenecks; tools like JProfiler or VisualVM can help with Java applicati...read more

Answered by AI
Add your answer
Q3. Given an array in rotated array find the element in that array
Ans. Find an element in a rotated sorted array using binary search for efficient lookup.
A rotated array is a sorted array that has been rotated at some pivot. Example: [4, 5, 6, 1, 2, 3].

Use binary search to find the target element. Check the mid-point and determine which half is sorted.

If the left half is sorted, check if the target is within that range. If not, search the right half.

Repeat the process until the element is...read more

Answered by AI
Add your answer
Q4. How do you handle push back from product and business teams?
Add your answer
Q5. How do you evaluate candidate beyond coding skills?
Ans. Evaluating candidates involves assessing soft skills, cultural fit, problem-solving abilities, and teamwork beyond coding proficiency.
Assess communication skills: Ask candidates to explain their thought process during coding challenges.

Evaluate problem-solving: Present real-world scenarios and ask how they would approach them.

Cultural fit: Discuss company values and see how candidates align with them.

Teamwork: Inquire ...read more

Answered by AI
Add your answer
Q6. Describe a production issue you handle yourself ?
Add your answer
Q7. How do you ensure backward compatiblity when shipping update on a feature which might be breaking
Ans. Ensuring backward compatibility involves careful planning, testing, and communication to avoid breaking existing functionality.
Versioning: Use semantic versioning to indicate breaking changes (e.g., from 1.0.0 to 2.0.0).

Deprecation: Mark features as deprecated before removal, providing alternatives and a timeline for users.

Feature Flags: Implement feature flags to toggle new features without affecting existing users.

Ex...read more

Answered by AI
Add your answer
Q8. How do you handle technical debt and how do you ensure that it is shipped without bugs
Ans. I prioritize addressing technical debt through structured processes and rigorous testing to ensure quality and reliability.
Regularly assess and prioritize technical debt during sprint planning, e.g., allocating time for refactoring legacy code.

Implement code reviews to catch potential bugs early, ensuring that new code adheres to quality standards.

Utilize automated testing frameworks to run unit and integration tests, e.g., using Jest for JavaScript applications.

Encourage team collaboration to identify and address technical debt collectively, fostering a culture of quality.

Document technical debt clearly in the project management tool, e.g., JIRA, to track and manage it effectively.




I appeared for an interview in Mar 2025, where I was asked the following questions.

Q1. Program to swap kth node from start and end in Linkedlist.
Ans. Swap the kth node from the start with the kth node from the end in a linked list.
Identify the kth node from the start and end of the linked list.

Traverse the list to find the kth node from the start.

Simultaneously traverse to find the kth node from the end.

Swap the values of the two identified nodes.

Consider edge cases: if k is greater than the length of the list.

Answered by AI
Add your answer
Q2. Minimum window substring.
Ans. Find the smallest substring in a string that contains all characters of another string.
Use two pointers to maintain a sliding window over the string.

Track character counts of the target string using a hash map.

Expand the window by moving the right pointer until all characters are included.

Shrink the window by moving the left pointer to find the minimum length.

Example: For s = 'ADOBECODEBANC' and t = 'ABC', the result is 'BANC'.

Answered by AI
Add your answer
Q3. Java Spring boot questions like inndoDB engine,Immutable class,Kafka basics,Profiles,Thread,Runnable , Sql query,GC, final finally finalize


Q1. Easy DSA questions on string manipulation


Q1. Build an MVC architecture
Ans. MVC architecture separates an application into Model, View, and Controller components for better organization and scalability.
Model represents the data and business logic

View displays the data to the user

Controller handles user input and updates the model

Example: Model - User class with properties like name, email; View - HTML template to display user info; Controller - User controller to handle user actions

Answered by AI
Add your answer
Round 3 - One-on-one 
(2 Questions)
Q1. Discussed previous job experiences.
Add your answer
Q2. Was asked if I am comfortable working on weekends/late nights during emergencies.
Add your answer
Round 4 - HR 
(1 Question)
Q1. What are your strengths/weaknesses.
Ans. Strengths - problem-solving, teamwork, communication. Weaknesses - perfectionism, time management.
Strengths: problem-solving - I enjoy tackling complex problems a...read more

Answered by AI


I appeared for an interview in Sep 2021.


Video Call - 1

Video Call - 2

HR
Round 1 - Video Call 
(2 Questions)
Round duration - 60 minutes
Round difficulty - Easy
Technical interview round where the interviewer asked me 2 DSA based problems.


Q1. Buy and Sell Stock Problem Statement
Imagine you are Harshad Mehta's friend, and you have been given the stock prices of a particular company for the next 'N' days. You can perform up to two buy-and-sell ...read more

Ans. The task is to determine the maximum profit that can be achieved by performing up to two buy-and-sell transactions on a given set of stock prices.
Iterate through the array of stock prices to find the maximum profit that can be achieved by buying and selling stocks at different points.

Keep track of the maximum profit that can be achieved by considering all possible combinations of buy and sell transactions.

Ensure that you sell the stock before you can buy again to adhere to the constraints of the problem.

Example: For input [3, 3, 5, 0, 0], the maximum profit possible is 6 by buying at 3 and selling at 5, then buying at 0 and selling at 0.

Answered by AI
Q2. Number of Bit Flips Problem Statement
Ninja is practicing binary representations and stumbled upon an interesting problem. Given two numbers 'A' and 'B', you are required to determine how many bits need t...read more

Ans. Calculate the number of bit flips required to convert one number to another in binary representation.
Convert both numbers to binary representation

Count the number...read more

Answered by AI
Round 2 - Video Call 
(2 Questions)
Round duration - 60 minutes
Round difficulty - Medium
Technical interview round where the interviewer asked me 2 DSA based problems.


Q1. Boundary Traversal of a Binary Tree
Given a binary tree of integers, your task is to return the boundary nodes of the tree in Anti-Clockwise direction starting from the root node.

Input:
The first line ...read more
Ans. Return the boundary nodes of a binary tree in Anti-Clockwise direction starting from the root node.
Traverse the left boundary nodes in top-down order

Traverse the ...read more

Answered by AI
Q2. Reverse Linked List in Groups of K
You are provided with a linked list containing 'N' nodes and an integer 'K'. The task is to reverse the linked list in groups of size K, which means reversing the nodes ...read more

Ans. Reverse a linked list in groups of size K by reversing nodes in each group.
Iterate through the linked list in groups of size K

Reverse each group of nodes

Handle ca...read more

Answered by AI
Round 3 - HR 
Round duration - 30 minutes
Round difficulty - Easy
Typical HR round where the interviewer asked behavioral problems.


Interview Advice from Real Candidates
Eligibility criteria
Above 7 CGPA
Info Edge India (Naukri.com) interview preparation:
Topics to prepare for the interview
- Data Structures, Algorithms, System Design, Aptitude, OOPS
Time required to prepare for the interview
- 6 months
Interview preparation tips for other job seekers
Tip 1 : Must do Previously asked Interview as well as Online Test Questions.
Tip 2 : Go through all the previous interview experiences from Codestudio and Leetcode.
Tip 3 : Do at-least 2 good projects and you must know every bit of them.

Application resume tips for other job seekers
Tip 1 : Have at-least 2 good projects explained in short with all important points covered.
Tip 2 : Every skill must be mentioned.
Tip 3 : Focus on skills, projects and experiences more.

Final outcome of the interview
Selected


Q1. 1. Count the number of flips require to convert a binary string of 0 and 1 such that resultant string has alternate 0 and 1
Q2. 2. From an array of integers which contains values for a particular stock . Find the value at which a person should buy and sell such that the profit is maximum.
Ans. Find the maximum profit from buying and selling a stock given an array of its values.
Iterate through the array and keep track of the minimum value seen so far.

Calculate the profit at each index and update the maximum profit seen.

Return the maximum profit.

Answered by AI
Add your answer
Q3. 3. Boundary traversal of a tree.
Ans. Boundary traversal of a tree
Boundary traversal involves visiting the nodes on the boundary of a tree in a specific order

Start with the root node and traverse the ...read more

Answered by AI
Add your answer
Q4. 4. Reverse a linked list in groups of k nodes.
Ans. Reverse a linked list in groups of k nodes.
Divide the linked list into groups of k nodes

Reverse each group of k nodes

Connect the reversed groups to form the final...read more

Answered by AI
Add your answer
Interview Advice from Real Candidates
Interview preparation tips
- 1. Focus on data structures and algorithm (2 rounds)
2. Technologies you are applying for and DSA could also be asked (3rd round 


Q1. Post fix to pre fix
Ans. Converting postfix expressions to prefix involves rearranging operators and operands based on their positions.
Postfix (Reverse Polish Notation) places operators after their operands, e.g., 'AB+' means 'A + B'.

Prefix (Polish Notation) places operators before their operands, e.g., '+AB' means 'A + B'.

To convert, use a stack: push operands, pop for operators, and rearrange accordingly.

Example: Postfix 'AB+C*' converts to Prefix '*+AB C'.

Answered by AI
Add your answer
Q2. Combination sum
Ans. Combination sum problem involves finding all unique combinations of numbers that sum up to a target value.
Use backtracking to explore all combinations.

Start with an empty combination and add numbers recursively.

Avoid duplicates by ensuring the same number isn't used multiple times in one combination.

Example: For candidates [2, 3, 6, 7] and target 7, valid combinations are [7] and [2, 2, 3].

Consider edge cases like empty candidates or target zero.


Q1. Circular linked list?
Q2. Topological sorting in graph
Ans. Topological sorting is a linear ordering of vertices in a directed acyclic graph where for every directed edge uv, vertex u comes before v.
Topological sorting is used in scheduling tasks, such as in project management or task scheduling algorithms.

It can be implemented using depth-first search (DFS) algorithm.

Kahn's algorithm is another popular method for topological sorting.

Example: Given a graph with vertices A, B, C and directed edges A->B, A->C, B->C, the topological sorting order could be A, B, C.

Q1. N-Queens, backtracking problem.
Add your answer
Q2. Longest increasing subsequence


sir 2 -3 month phle feature release hua tha...ha mene dekha yeh...phle yeh primary node pe read kr rha tha data ...ho skta hai ek dam se load aaya primary node toh sab crash kr gya ho...apne esko secondary pe shift  kra apne hai...