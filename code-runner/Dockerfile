FROM docker:27-cli

RUN apk add --no-cache python3

WORKDIR /app
COPY runner.py /app/runner.py

EXPOSE 8090
ENTRYPOINT ["python3", "/app/runner.py"]
