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
Application keeps separate collection for every group. Every such collection has an descending index on `created` field, so reading group feed is very fast.
To read user's feed I use `MergeSortedN` component, which extends `Graph` from Akka Streams, so getting many group feed is still in complexity `O(n*log(k))`, where n is max a number of posts in a group and k is a number of groups.
