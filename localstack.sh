#!/bin/sh
awslocal s3api create-bucket --bucket videos
awslocal sqs create-queue --queue-name videos-persistidos-dlq
awslocal sqs create-queue --queue-name videos-persistidos
awslocal sqs set-queue-attributes \
    --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/videos-persistidos \
    --attributes '{
    "RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:videos-persistidos-dlq\",\"maxReceiveCount\":\"3\"}"
}'
awslocal dynamodb create-table \
    --table-name videos \
    --key-schema AttributeName=idUsuario,KeyType=HASH AttributeName=nomeVideo,KeyType=RANGE \
    --attribute-definitions AttributeName=idUsuario,AttributeType=S AttributeName=nomeVideo,AttributeType=S\
    --billing-mode PAY_PER_REQUEST
