# Feeds
simple app for showing groups feed, using Akka Streams and MongoDB.

## How to run
To run the application, you need a MongoDB instance, running on `localhost:27017` port (one can change it in `application.conf`).
You can get it for example with docker, by running command `docker run -d -p 27017-27019:27017-27019 --name mongodb mongo:latest`
After that, it's sufficient to launch application by command `sbt run`

## My Assumptions
I assumed that probably one user won't be a participant of many groups. It is number of user's groups is probably much smaller than number of posts in every group.
On the other hand it's normal for groups to have very many users.
## About
Application keeps separate mongodb collection for every group. Every such collection has an descending index on `created` field, so reading group feed is fast.
To read user's feed I use `MergeSortedN` component, which extends `Graph` from Akka Streams, so getting many group feed is still in complexity `O(n*log(k))`, where n is max a number of posts in a group and k is a number of groups. Because according to my assumptions k is much smaller than n, the actual time of processing should be similar to group-feed processing time.
Responses for post requests are sent in chunked form.

##API
You should attach a header with `Auth-Token`, containing an userId to your requests. You can find following endpoints:
```
GET     /get-posts                            Lists all posts from all user's groups. The response is chunked.

GET     /groups/list                          Lists all groups which user is member of
GET     /groups/:id/get-posts                 Lists posts from a group (User must be a member of a group to read or write to it!). The response is chunked.


POST     /groups/:id/join                     Adds user to particular group, given by id

POST     /groups/:id/add-post                 Add post to a group. request body should contain a json "{"content": post_content}

POST      /users/set-name                      sets username (not obligatory, if not set, user is given a default name)
```
example request (curl):
```
 curl -i --request POST -H 'Content-Type: application/json' --header 'Auth-Token: 100' \
--data '{"content": "some content of a post"}' \
-k http://localhost:9000/groups/6/add-post
```

