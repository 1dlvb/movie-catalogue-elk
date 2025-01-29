#!/bin/bash

echo "Checking if index exists..."
INDEX_EXISTS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:9200/movie.public.movie")

if [ "$INDEX_EXISTS" -eq 200 ]; then
  echo "Index exists. Proceeding with re-creating."

  echo "Deleting old index..."
  curl -X DELETE "http://localhost:9200/movie.public.movie"

  echo "Creating new index..."
  curl -X PUT "http://localhost:9200/movie.public.movie" \
  -H 'Content-Type: application/json' \
  -d @config/index-config.json
else
  echo "Index does not exist. Creating new index..."
  curl -X PUT "http://localhost:9200/movie.public.movie" \
  -H 'Content-Type: application/json' \
  -d @config/index-config.json
fi

check_connector_exists() {
  CONNECTOR_NAME=$1
  RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8083/connectors/$CONNECTOR_NAME")
  if [ "$RESPONSE" -eq 200 ]; then
    echo "Connector $CONNECTOR_NAME already exists."
    return 0
  else
    echo "Connector $CONNECTOR_NAME does not exist."
    return 1
  fi
}

delete_connector() {
  CONNECTOR_NAME=$1
  echo "Deleting connector $CONNECTOR_NAME..."
  curl -X DELETE "http://localhost:8083/connectors/$CONNECTOR_NAME"
}

echo "Configuring PostgreSQL connector..."
if [ -f config/postgresql-connector-config.json ]; then
  if check_connector_exists "postgresql-connector"; then
    delete_connector "postgresql-connector"
  fi
  echo "Creating PostgreSQL connector..."
  curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @config/postgresql-connector-config.json
else
  echo "File config/postgresql-connector-config.json not found"
fi

echo "Configuring Elasticsearch Sink connector..."
if [ -f config/elasticsearch-sink.json ]; then
  if check_connector_exists "elasticsearch-sink"; then
    delete_connector "elasticsearch-sink"
  fi
  echo "Creating Elasticsearch Sink connector..."
  curl -X POST -H "Content-Type: application/json" \
  --data @config/elasticsearch-sink.json \
  http://localhost:8083/connectors
else
  echo "File config/elasticsearch-sink.json not found"
fi
