# Cổng LiteLLM cho Trợ lý FixLink (dùng khi deploy, ví dụ Render)
FROM ghcr.io/berriai/litellm:main-stable
COPY config.yaml /app/config.yaml
COPY entrypoint.sh /app/fixlink-entrypoint.sh
EXPOSE 4000
ENTRYPOINT ["/bin/sh", "/app/fixlink-entrypoint.sh"]
CMD ["--config", "/app/config.yaml", "--port", "4000"]
