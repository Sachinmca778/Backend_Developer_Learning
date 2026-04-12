# HATEOAS Basics

## Status: Not Started

---

## Table of Contents

1. [HATEOAS Overview](#hateoas-overview)
2. [spring-boot-starter-hateoas](#spring-boot-starter-hateoas)
3. [EntityModel](#entitymodel)
4. [CollectionModel](#collectionmodel)
5. [WebMvcLinkBuilder](#webmvclinkbuilder)
6. [Self Links](#self-links)
7. [Related Resource Links](#related-resource-links)

---

## HATEOAS Overview

**HATEOAS** = **Hypermedia As The Engine Of Application State**

REST ki ek maturity level hai — API responses mein **links** include hote hain jo client ko batate hain ki woh aage kya kar sakta hai.

### REST Maturity Levels

```
Level 0: POX (Plain Old XML) — HTTP as transport
Level 1: Resources — Proper URIs
Level 2: HTTP Verbs — GET, POST, PUT, DELETE
Level 3: HATEOAS — Hypermedia controls (links in responses)
```

### Without HATEOAS

```json
GET /api/users/1

{
  "id": 1,
  "name": "Sachin",
  "email": "sachin@example.com"
}
```

**Problem:** Client ko manually URL construct karna padta hai related resources ke liye.

### With HATEOAS

```json
GET /api/users/1

{
  "id": 1,
  "name": "Sachin",
  "email": "sachin@example.com",
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/users/1"
    },
    "posts": {
      "href": "http://localhost:8080/api/users/1/posts"
    },
    "delete": {
      "href": "http://localhost:8080/api/users/1",
      "method": "DELETE"
    }
  }
}
```

**Fayda:**
- Client ko URLs manually construct nahi karne padte
- API discoverable ban jaata hai
- URL changes hone pe client code change nahi karna padta

---

## spring-boot-starter-hateoas

### Setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-hateoas</artifactId>
</dependency>
```

### What It Provides

| Component | Purpose |
|-----------|---------|
| **EntityModel<T>** | Single resource + links |
| **CollectionModel<T>** | Collection of resources + links |
| **WebMvcLinkBuilder** | Links build karna (method references se) |
| **RepresentationModel** | Base class for custom models |
| **PagedModel** | Paginated collection + links |

---

## EntityModel

**Matlab:** Ek single resource (entity) ko wrap karta hai aur uske saath links add karta hai.

### Basic Usage

```java
@GetMapping("/users/{id}")
public EntityModel<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);

    // EntityModel.create — entity + links
    return EntityModel.of(user,
        linkTo(methodOn(UserController.class).getUser(id)).withSelfRel(),
        linkTo(methodOn(UserController.class).getAllUsers()).withRel("users"),
        linkTo(methodOn(UserController.class).getUserPosts(id)).withRel("posts")
    );
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Sachin",
  "email": "sachin@example.com",
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/users/1"
    },
    "users": {
      "href": "http://localhost:8080/api/users"
    },
    "posts": {
      "href": "http://localhost:8080/api/users/1/posts"
    }
  }
}
```

### Manual EntityModel Creation

```java
@GetMapping("/users/{id}")
public EntityModel<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);

    EntityModel<User> resource = EntityModel.of(user);

    // Self link
    resource.add(linkTo(methodOn(UserController.class).getUser(id)).withSelfRel());

    // Related resources
    resource.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"));
    resource.add(linkTo(methodOn(PostController.class).getUserPosts(id)).withRel("posts"));

    return resource;
}
```

---

## CollectionModel

**Matlab:** Multiple resources (collection) ko wrap karta hai aur collection-level links add karta hai.

### Basic Usage

```java
@GetMapping("/users")
public CollectionModel<EntityModel<User>> getAllUsers() {
    List<User> users = userService.findAll();

    // Har user ko EntityModel mein wrap karo
    List<EntityModel<User>> userResources = users.stream()
        .map(user -> EntityModel.of(user,
            linkTo(methodOn(UserController.class).getUser(user.getId())).withSelfRel()
        ))
        .collect(Collectors.toList());

    // CollectionModel — collection + links
    return CollectionModel.of(userResources,
        linkTo(methodOn(UserController.class).getAllUsers()).withSelfRel(),
        linkTo(methodOn(UserController.class).createUser(null)).withRel("create-user")
    );
}
```

**Response:**
```json
{
  "_embedded": {
    "userList": [
      {
        "id": 1,
        "name": "Sachin",
        "email": "sachin@example.com",
        "_links": {
          "self": {
            "href": "http://localhost:8080/api/users/1"
          }
        }
      },
      {
        "id": 2,
        "name": "Rahul",
        "email": "rahul@example.com",
        "_links": {
          "self": {
            "href": "http://localhost:8080/api/users/2"
          }
        }
      }
    ]
  },
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/users"
    },
    "create-user": {
      "href": "http://localhost:8080/api/users"
    }
  }
}
```

---

## WebMvcLinkBuilder

**Matlab:** Links build karne ka utility class. Method references use karta hai — type-safe approach.

### Static Imports

```java
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
```

### Basic Usage

```java
// Self link — current resource ka URL
linkTo(methodOn(UserController.class).getUser(1L)).withSelfRel()

// Related link — dusre resource ka URL
linkTo(methodOn(PostController.class).getAllPosts()).withRel("posts")

// With path variable
linkTo(methodOn(UserController.class).getUserPosts(1L)).withRel("user-posts")
```

### Conditional Links

```java
@GetMapping("/users/{id}")
public EntityModel<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);

    EntityModel<User> resource = EntityModel.of(user);

    // Self link (hamesha)
    resource.add(linkTo(methodOn(UserController.class).getUser(id)).withSelfRel());

    // Delete link — sirf agar user admin hai
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
        resource.add(linkTo(methodOn(UserController.class).deleteUser(id))
            .withRel("delete")
            .withType("DELETE"));
    }

    return resource;
}
```

### Multiple Methods Chain

```java
// Slash-based path building
Link ordersLink = linkTo(methodOn(OrderController.class).getOrders(userId))
    .slash("items")
    .withRel("order-items");
```

---

## Self Links

**Matlab:** Resource ka khud ka URL — "yeh resource kahan hai" batata hai.

### withSelfRel()

```java
@GetMapping("/users/{id}")
public EntityModel<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);

    return EntityModel.of(user,
        // "self" link — resource ka apna URL
        linkTo(methodOn(UserController.class).getUser(id)).withSelfRel()
    );
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Sachin",
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/users/1"
    }
  }
}
```

### Self Link with Custom Relation Name

```java
Link userLink = linkTo(methodOn(UserController.class).getUser(id))
    .withRel("user-profile");  // Custom name instead of "self"
```

---

## Related Resource Links

**Matlab:** Resource se related dusre resources ke links — "aage kya kar sakte ho" batata hai.

### Single Resource with Related Links

```java
@GetMapping("/users/{id}")
public EntityModel<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);

    return EntityModel.of(user,
        // Self
        linkTo(methodOn(UserController.class).getUser(id)).withSelfRel(),

        // Related: User ke posts
        linkTo(methodOn(PostController.class).getUserPosts(id)).withRel("posts"),

        // Related: User ke orders
        linkTo(methodOn(OrderController.class).getUserOrders(id)).withRel("orders"),

        // Related: User ka profile
        linkTo(methodOn(UserController.class).getUserProfile(id)).withRel("profile")
    );
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Sachin",
  "_links": {
    "self": { "href": "http://localhost:8080/api/users/1" },
    "posts": { "href": "http://localhost:8080/api/users/1/posts" },
    "orders": { "href": "http://localhost:8080/api/users/1/orders" },
    "profile": { "href": "http://localhost:8080/api/users/1/profile" }
  }
}
```

### Collection with Related Links

```java
@GetMapping("/posts/{postId}/comments")
public CollectionModel<EntityModel<Comment>> getPostComments(@PathVariable Long postId) {
    List<Comment> comments = commentService.findByPostId(postId);

    List<EntityModel<Comment>> commentResources = comments.stream()
        .map(comment -> EntityModel.of(comment,
            linkTo(methodOn(CommentController.class).getPostComment(postId, comment.getId()))
                .withSelfRel(),
            linkTo(methodOn(CommentController.class).updatePostComment(postId, comment.getId(), null))
                .withRel("update")
                .withType("PUT"),
            linkTo(methodOn(CommentController.class).deletePostComment(postId, comment.getId()))
                .withRel("delete")
                .withType("DELETE")
        ))
        .collect(Collectors.toList());

    return CollectionModel.of(commentResources,
        linkTo(methodOn(CommentController.class).getPostComments(postId)).withSelfRel(),
        linkTo(methodOn(CommentController.class).createPostComment(postId, null))
            .withRel("create")
            .withType("POST"),
        linkTo(methodOn(PostController.class).getPost(postId)).withRel("post")
    );
}
```

### Paginated Collection with Links

```java
@GetMapping("/users")
public PagedModel<EntityModel<User>> getUsers(Pageable pageable) {
    Page<User> userPage = userService.findAll(pageable);

    // PagedModel — paginated data + pagination links
    return pagedModelOf(userPage.map(user -> EntityModel.of(user,
            linkTo(methodOn(UserController.class).getUser(user.getId())).withSelfRel()
        )),
        linkTo(methodOn(UserController.class).getUsers(pageable)).withSelfRel()
    );
}
```

**Response:**
```json
{
  "_embedded": {
    "userList": [...]
  },
  "_links": {
    "self": { "href": "http://localhost:8080/api/users?page=0&size=10" },
    "next": { "href": "http://localhost:8080/api/users?page=1&size=10" },
    "prev": { "href": "http://localhost:8080/api/users?page=0&size=10" }
  },
  "page": {
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "number": 0
  }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **HATEOAS** | Responses mein links include karo — API discoverable banta hai |
| **spring-boot-starter-hateoas** | HATEOAS support enable karne ka dependency |
| **EntityModel<T>** | Single resource + links wrap karo |
| **CollectionModel<T>** | Collection of resources + links wrap karo |
| **WebMvcLinkBuilder** | `linkTo(methodOn(...))` se type-safe links banao |
| **withSelfRel()** | Resource ka khud ka URL ("self" link) |
| **withRel("name")** | Related resource links ("posts", "orders", etc.) |
| **PagedModel** | Paginated collection + prev/next/first/last links |
