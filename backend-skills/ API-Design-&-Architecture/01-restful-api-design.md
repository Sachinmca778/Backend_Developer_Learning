# RESTful API Design

**Date:** 05 April 2025  
**Status:** Done

## What I understood

REST is a set of rules for designing APIs. The main idea is
that your URL should represent a "thing" (noun), not an 
action (verb). The HTTP method tells what action to do.

Wrong: /getUser /deleteUser/5  
Right: /users   /users/5

## 6 Rules of REST (simple version)

- **Stateless** — server remembers nothing between requests.
  That's why we send JWT token in every request.
- **Client-Server** — frontend and backend are separate.
- **Cacheable** — tell client if response can be cached.
- **Uniform Interface** — everyone follows same URL pattern.
- **Layered** — client doesn't know about load balancer etc.
                Client → Load Balancer → API Gateway → Spring Boot → Database
                (client ko sirf pata hai apna endpoint)
- **Code on demand** — optional, rarely used. (Server executable code bhi bhej sakta hai (jaise JavaScript))

## Resource Naming — what I will follow
```
/users              all users
/users/5            one user
/users/5/orders     orders of user 5
```

Rules I'll remember:
- Always plural  → /users not /user
- Always lowercase → /blog-posts not /blogPosts
- No verbs in URL → /users not /getUsers

## HTTP Methods
```
GET    → read only, no change
POST   → create new
PUT    → replace complete object
PATCH  → update only few fields
DELETE → delete
```

## PUT vs PATCH — main difference

PUT — I have to send ALL fields. If I forget one field,
it becomes null. Dangerous.

PATCH — I send only what I want to change. Rest stays same.

Real example I understood:
- User has name, email, city, phone
- I want to change only name
- PUT: send all 4 fields or others become null
- PATCH: send only name, done

## Idempotency

Same request sent 100 times = same result.

- GET → idempotent (reading same data)
- PUT → idempotent (replacing with same data)
- DELETE → idempotent (already deleted, still 404)
- POST → NOT idempotent (creates new record each time)
- PATCH → NOT idempotent (creates new record each time)
PATCH  /users/5  →  depends   ⚠️  (usually idempotent)

Real world example — Payment:
User ne "Pay ₹500" button dabaya
Network slow tha → request 3 baar gayi

POST /payments   ← 3 baar gaya = ₹1500 kat gaya ❌

Solution: Idempotency Key use karo
POST /payments
Idempotency-Key: unique-uuid-here

Server check karta hai — yeh key pehle dekhi hai?
Haan → same response return karo, dobara charge mat karo ✅


This is why payment APIs use Idempotency-Key header —
so accidental double click doesn't charge twice.

## Status codes I will remember

```
2xx — Success
200 OK              → GET, PUT, PATCH successful
201 Created         → POST se naya resource bana
204 No Content      → DELETE successful (body nahi hoti)

3xx — Redirect
301 Moved Permanently  → URL change ho gayi hamesha ke liye
302 Found              → Temporarily alag jagah hai
304 Not Modified       → Cache valid hai, naya data nahi

4xx — Client ki galti
400 Bad Request        → Request format galat hai
401 Unauthorized       → Login nahi kiya
403 Forbidden          → Login kiya hai but permission nahi
404 Not Found          → Resource exist nahi karta
405 Method Not Allowed → GET chahiye tha, POST bhej diya
409 Conflict           → Email already exist karta hai
410 Gone               → Resource permanently delete ho gaya
422 Unprocessable      → Validation fail
429 Too Many Requests  → Rate limit exceed

5xx — Server ki galti
500 Internal Server Error  → Server crash ho gaya
502 Bad Gateway            → Upstream server ne galat response diya
503 Service Unavailable    → Server down/overloaded
504 Gateway Timeout        → Upstream server ne time pe respond nahi kiya
```

## Safe Methods

Safe = no data changes on server.
GET, HEAD, OPTIONS are safe.
POST, PUT, PATCH, DELETE are not safe.

HEAD is useful to check if a file exists 
without downloading it.

# Check karo file exist karti hai ya nahi — poori download kiye bina
HEAD /files/report.pdf

# Response mein sirf headers aate hain:
Content-Length: 2048576
Content-Type: application/pdf
Last-Modified: Mon, 01 Apr 2025 10:00:00 GMT
// Body nahi aata — bandwidth bachti hai

## Code I wrote myself
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    public List<UserResponse> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse getOne(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody @Valid CreateUserRequest req) {
        return userService.create(req);
    }

    @PutMapping("/{id}")
    public UserResponse replace(@PathVariable Long id,
                                @RequestBody @Valid UpdateUserRequest req) {
        return userService.replace(id, req);
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Long id,
                               @RequestBody PatchUserRequest req) {
        return userService.patch(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

## Still confused about

- [ ] When exactly to use 400 vs 422?
- [ ] How to handle PATCH when field is intentionally 
      set to null vs field not sent at all?
- [ ] HATEOAS — is it actually used in real projects?

## Tomorrow I will do

- [ ] Build this User CRUD API in Spring Boot
- [ ] Test all 6 endpoints in Postman
- [ ] Try sending PUT without all fields — see null happen
- [ ] Try PATCH with only one field — confirm rest stays same