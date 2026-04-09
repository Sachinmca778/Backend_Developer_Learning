# GraphQL Basics

## Status: Not Started

---

## Table of Contents

1. [What is GraphQL?](#what-is-graphql)
2. [REST vs GraphQL](#rest-vs-graphql)
3. [Queries](#queries)
4. [Mutations](#mutations)
5. [Subscriptions](#subscriptions)
6. [Schema Definition Language (SDL)](#schema-definition-language-sdl)
7. [Resolvers](#resolvers)
8. [N+1 Problem & DataLoader](#n1-problem--dataloader)
9. [GraphQL Java & Spring GraphQL](#graphql-java--spring-graphql)
10. [REST vs GraphQL Trade-offs](#rest-vs-graphql-trade-offs)

---

## What is GraphQL?

**GraphQL kya hai?**
Facebook ne banaya ek query language for APIs - client exactly wohi data maangta hai jo chahiye, na zyada na kam.

**REST mein problem:**
- `/api/users/1` - Fixed response (id, name, email, address, posts, comments...)
- Agar sirf `name` chahiye toh bhi sab data aayega (**Over-fetching**)
- Agar `name` + `posts` + `comments` chahiye toh multiple requests lagani padengi (**Under-fetching**)

**GraphQL solution:**
```graphql
# Client exactly specify karta hai kya chahiye
query {
  user(id: 1) {
    name
    posts {
      title
      comments {
        text
      }
    }
  }
}
```

**Response:** Sirf requested data aayega - no over-fetching, no under-fetching.

---

## REST vs GraphQL

| Feature | REST | GraphQL |
|---------|------|---------|
| **Endpoints** | Multiple (`/users`, `/posts`, `/comments`) | Single (`/graphql`) |
| **Data Fetching** | Fixed response structure | Client decides what data chahiye |
| **Over-fetching** | Haan (extra data aata hai) | Nahi (sirf requested data) |
| **Under-fetching** | Haan (multiple requests lagti hain) | Nahi (ek request mein sab) |
| **Versioning** | `/api/v1/users`, `/api/v2/users` | Schema evolve hota hai (deprecated fields) |
| **Caching** | HTTP caching built-in | Khud handle karna padta hai |
| **Learning Curve** | Easy | Thoda complex |
| **Error Handling** | HTTP status codes | `errors` array in response |

### Example Comparison

**REST (3 requests):**
```
GET /api/users/1
GET /api/users/1/posts
GET /api/posts/1/comments
```

**GraphQL (1 request):**
```graphql
query {
  user(id: 1) {
    name
    posts {
      title
      comments {
        text
      }
    }
  }
}
```

---

## Queries

**Queries data fetch karne ke liye hote hain** - GET equivalent.

### Basic Query
```graphql
query {
  user(id: 1) {
    name
    email
  }
}
```

**Response:**
```json
{
  "data": {
    "user": {
      "name": "John Doe",
      "email": "john@example.com"
    }
  }
}
```

### Query with Variables
```graphql
query GetUser($userId: ID!) {
  user(id: $userId) {
    name
    email
  }
}
```

**Variables:**
```json
{
  "userId": 1
}
```

### Multiple Fields
```graphql
query {
  user(id: 1) {
    name
    email
    age
    posts {
      title
      createdAt
    }
  }
}
```

### Query with Arguments
```graphql
query {
  posts(category: "tech", limit: 10, offset: 0) {
    id
    title
    author {
      name
    }
  }
}
```

---

## Mutations

**Mutations data modify karne ke liye hote hain** - POST/PUT/DELETE equivalent.

### Create Mutation
```graphql
mutation {
  createUser(input: {
    name: "John Doe"
    email: "john@example.com"
    age: 25
  }) {
    id
    name
    email
  }
}
```

### Update Mutation
```graphql
mutation {
  updateUser(id: 1, input: {
    name: "John Updated"
  }) {
    id
    name
    email
  }
}
```

### Delete Mutation
```graphql
mutation {
  deleteUser(id: 1) {
    success
    message
  }
}
```

**Response:**
```json
{
  "data": {
    "deleteUser": {
      "success": true,
      "message": "User deleted successfully"
    }
  }
}
```

---

## Subscriptions

**Subscriptions real-time data ke liye hote hain** - WebSocket use karta hai.

```graphql
subscription {
  postCreated {
    id
    title
    author {
      name
    }
    createdAt
  }
}
```

**Jab bhi koi naya post create karega, client ko automatically data push hoga:**
```json
{
  "data": {
    "postCreated": {
      "id": 42,
      "title": "New Post",
      "author": { "name": "John" },
      "createdAt": "2024-01-01T10:00:00Z"
    }
  }
}
```

**Use Cases:**
- Live notifications
- Chat messages
- Stock price updates
- Live sports scores

---

## Schema Definition Language (SDL)

**SDL GraphQL ka type system hai** - define karta hai ki API kya data support karta hai.

### Object Types
```graphql
type User {
  id: ID!
  name: String!
  email: String!
  age: Int
  posts: [Post!]!
}

type Post {
  id: ID!
  title: String!
  content: String
  author: User!
  comments: [Comment!]!
  createdAt: String!
}

type Comment {
  id: ID!
  text: String!
  author: User!
}
```

### Type Modifiers
| Syntax | Matlab |
|--------|--------|
| `String` | Nullable (null ho sakta hai) |
| `String!` | Non-null (null nahi ho sakta) |
| `[String]` | List of nullable strings |
| `[String!]` | List of non-null strings |
| `[String!]!` | Non-null list of non-null strings |

### Query Type (Entry Point for Reads)
```graphql
type Query {
  user(id: ID!): User
  users(limit: Int, offset: Int): [User!]!
  posts(category: String): [Post!]!
  post(id: ID!): Post
}
```

### Mutation Type (Entry Point for Writes)
```graphql
type Mutation {
  createUser(input: CreateUserInput!): User!
  updateUser(id: ID!, input: UpdateUserInput!): User!
  deleteUser(id: ID!): DeleteResponse!
  createPost(input: CreatePostInput!): Post!
}
```

### Subscription Type
```graphql
type Subscription {
  postCreated: Post!
  userUpdated(id: ID!): User!
  newComment(postId: ID!): Comment!
}
```

### Input Types
```graphql
input CreateUserInput {
  name: String!
  email: String!
  age: Int
}

input UpdateUserInput {
  name: String
  email: String
  age: Int
}
```

### Enums
```graphql
enum Role {
  USER
  ADMIN
  MODERATOR
}

enum PostStatus {
  DRAFT
  PUBLISHED
  ARCHIVED
}
```

### Interfaces
```graphql
interface Node {
  id: ID!
}

type User implements Node {
  id: ID!
  name: String!
}

type Post implements Node {
  id: ID!
  title: String!
}
```

### Union Types
```graphql
union SearchResult = User | Post | Comment

type Query {
  search(query: String!): [SearchResult!]!
}
```

---

## Resolvers

**Resolvers actual data fetch karne wale functions hote hain** - har field ka resolver hota hai.

### Java/Spring GraphQL Resolver Example

```java
@Controller
public class UserController {
    
    private final UserRepository userRepository;
    
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    // Query resolver
    @QueryMapping
    public User user(@Argument Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    // List query resolver
    @QueryMapping
    public List<User> users(@Argument int limit, @Argument int offset) {
        return userRepository.findAll(PageRequest.of(offset, limit)).getContent();
    }
    
    // Mutation resolver
    @MutationMapping
    public User createUser(@Argument CreateUserInput input) {
        User user = new User();
        user.setName(input.name());
        user.setEmail(input.email());
        return userRepository.save(user);
    }
}
```

### Nested Resolvers
```java
@Controller
public class UserResolver {
    
    private final PostRepository postRepository;
    
    public UserResolver(PostRepository postRepository) {
        this.postRepository = postRepository;
    }
    
    // User.posts field resolver
    @SchemaMapping(typeName = "User", field = "posts")
    public List<Post> getPosts(User user) {
        return postRepository.findByUserId(user.getId());
    }
}
```

### Resolver Execution Flow
```
query {
  user(id: 1) {          ← Query resolver call hoga
    name                 ← Direct field (no resolver needed)
    posts {              ← Field resolver call hoga (user object se)
      title              ← Direct field
      author {           ← Field resolver call hoga (post object se)
        name             ← Direct field
      }
    }
  }
}
```

---

## N+1 Problem & DataLoader

### N+1 Problem Kya Hai?

**Problem:** Har nested field ke liye alag database query chalti hai - performance bahut slow ho jaati hai.

```graphql
query {
  users {              # 1 query: SELECT * FROM users
    posts {            # N queries: SELECT * FROM posts WHERE user_id = ? (har user ke liye)
      title
      author {         # N queries: SELECT * FROM users WHERE id = ? (har post ke liye)
        name
      }
    }
  }
}
```

**10 users hain, har user ke 5 posts hain:**
- 1 query (users)
- 10 queries (posts - har user ke liye)
- 50 queries (authors - har post ke liye)
- **Total: 61 queries! (N+1 problem)**

### Solution: DataLoader (Batching)

**DataLoader requests ko batch karke ek query mein convert kar deta hai.**

```java
@Configuration
public class DataLoaderConfig {
    
    @Bean
    public DataLoader<Long, Post> postsByUserLoader(PostRepository postRepository) {
        return DataLoader.newDataLoader(userIds -> {
            // Ek query mein sab posts fetch karo
            List<Post> posts = postRepository.findByUserIdIn(userIds);
            
            // Map banao: userId -> List<Post>
            Map<Long, List<Post>> postsByUser = posts.stream()
                .collect(Collectors.groupingBy(Post::getUserId));
            
            // Ordered results return karo
            return CompletableFuture.completedFuture(
                userIds.stream()
                    .map(id -> postsByUser.getOrDefault(id, List.of()))
                    .toList()
            );
        });
    }
}
```

### Using DataLoader in Resolver
```java
@Controller
public class UserResolver {
    
    @SchemaMapping
    public CompletableFuture<List<Post>> posts(User user, DataLoader<Long, List<Post>> postsLoader) {
        return postsLoader.load(user.getId());  // Batch ho jayega
    }
}
```

### Before DataLoader
```
# 10 users → 10 separate queries
SELECT * FROM posts WHERE user_id = 1
SELECT * FROM posts WHERE user_id = 2
SELECT * FROM posts WHERE user_id = 3
... (10 queries total)
```

### After DataLoader
```
# 10 users → 1 batched query
SELECT * FROM posts WHERE user_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
```

**61 queries → 2 queries! Massive performance improvement.**

---

## GraphQL Java & Spring GraphQL

### Option 1: graphql-java (Low-level)

**Dependency:**
```xml
<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java</artifactId>
</dependency>
```

**Setup:**
```java
@Configuration
public class GraphQLConfig {
    
    @Bean
    public GraphQL graphQL(UserRepository userRepository) {
        GraphQLSchema schema = GraphQLSchema.newSchema()
            .query(GraphQLObjectType.newObject()
                .name("Query")
                .field(field -> field
                    .name("user")
                    .type(GraphQLTypeReference.typeRef("User"))
                    .argument(GraphQLArgument.newArgument()
                        .name("id")
                        .type(GraphQLNonNull.nonNull(GraphQLID)))
                    .dataFetcher(environment -> {
                        Long id = environment.getArgument("id");
                        return userRepository.findById(id).orElse(null);
                    }))
            .build();
        
        return GraphQL.newGraphQL(schema).build();
    }
}
```

**Verdict:** Bahut boilerplate - prefer Spring GraphQL.

---

### Option 2: Spring for GraphQL (Recommended)

**Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>
```

**Schema File:**
```graphql
# src/main/resources/graphql/graphql-schema.graphqls
type Query {
  user(id: ID!): User
  users: [User!]!
}

type Mutation {
  createUser(input: CreateUserInput!): User!
}

type User {
  id: ID!
  name: String!
  email: String!
}

input CreateUserInput {
  name: String!
  email: String!
}
```

**Controller:**
```java
@Controller
@GraphQLApi
public class UserController {
    
    private final UserRepository userRepository;
    
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @QueryMapping
    public User user(@Argument Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    @QueryMapping
    public List<User> users() {
        return userRepository.findAll();
    }
    
    @MutationMapping
    public User createUser(@Argument CreateUserInput input) {
        User user = new User();
        user.setName(input.name());
        user.setEmail(input.email());
        return userRepository.save(user);
    }
}
```

**GraphQL Endpoint:**
```
POST http://localhost:8080/graphql
```

**GraphiQL (Interactive UI):**
```yaml
# application.yml
spring:
  graphql:
    graphiql:
      enabled: true
      path: /graphiql
```

Access: `http://localhost:8080/graphiql`

---

## REST vs GraphQL Trade-offs

### GraphQL Choose Karo Jab:

| Scenario | Why GraphQL |
|----------|-------------|
| **Mobile apps** | Bandwidth kam chahiye (sirf requested data) |
| **Complex nested data** | Ek request mein multiple resources |
| **Multiple client types** | Web, mobile, tablet - sab alag data maangte hain |
| **Rapid frontend iteration** | Backend change kiye bina frontend data structure change |
| **Real-time features** | Subscriptions built-in |

### REST Choose Karo Jab:

| Scenario | Why REST |
|----------|----------|
| **Simple CRUD** | GraphQL overkill hai |
| **Caching important hai** | HTTP caching built-in, GraphQL mein manually karna padta hai |
| **File uploads** | REST easier (GraphQL mein extra setup chahiye) |
| **Team familiarity** | REST sabko aata hai, GraphQL learning curve hai |
| **Microservices** | Har service ka alag REST API simple rehta hai |

### Comparison Summary

| Factor | REST | GraphQL |
|--------|------|---------|
| **Flexibility** | Low (fixed responses) | High (client chooses) |
| **Performance** | Over/under-fetching possible | Optimized per request |
| **Caching** | ✅ HTTP caching | ❌ Manual |
| **Error Handling** | HTTP status codes | `errors` array |
| **File Uploads** | ✅ Easy | ⚠️ Extra setup |
| **Versioning** | `/v1/`, `/v2/` | Schema evolution |
| **Tooling** | Mature | Growing |
| **Learning Curve** | Easy | Moderate |

---

## Quick Reference

```graphql
# Query
query { user(id: 1) { name email } }

# Mutation
mutation { createUser(input: { name: "John" }) { id name } }

# Subscription
subscription { postCreated { title } }

# Schema
type Query { users: [User!]! }
type Mutation { createUser(input: UserInput!): User! }
type User { id: ID!, name: String! }

# Type Modifiers
String       # Nullable
String!      # Non-null
[String]     # List of nullable
[String!]!   # Non-null list of non-null
```

```java
// Spring GraphQL
@QueryMapping
@MutationMapping
@SchemaMapping(typeName = "User", field = "posts")
@DataLoader
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Queries** | Data fetch (GET equivalent) |
| **Mutations** | Data modify (POST/PUT/DELETE equivalent) |
| **Subscriptions** | Real-time updates (WebSocket) |
| **SDL** | Schema definition language - type system |
| **Resolvers** | Actual data fetch functions |
| **N+1 Problem** | Har nested field pe alag query - slow performance |
| **DataLoader** | Batching se N+1 solve karta hai |
| **Spring GraphQL** | Spring Boot + GraphQL integration (recommended) |
| **REST vs GraphQL** | REST = Simple/Caching, GraphQL = Flexibility/Performance |
