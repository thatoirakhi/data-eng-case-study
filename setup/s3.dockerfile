FROM docker.io/minio/minio:latest AS custom-minio

COPY --from=docker.io/minio/mc:latest /usr/bin/mc /usr/bin/mc
RUN mkdir /usr/data
ADD data /usr/data/taxi-data
ADD reference-data /usr/data/reference-data

RUN mkdir /buckets
RUN minio server /buckets & \
    server_pid=$!; \
    until mc alias set local http://localhost:9000 minioadmin minioadmin; do \
        sleep 1; \
    done; \
    mc mb rm -r --force local/taxi-bucket; \
    mc mb local/taxi-bucket; \
    mc policy set public minio/warehouse; \
    mc cp -r /usr/data/taxi-data local/taxi-bucket; \
    mc cp -r /usr/data/reference-data local/taxi-bucket; \
    kill $server_pid

CMD ["minio", "server", "/buckets", "--address", ":9000", "--console-address", ":9001"]
