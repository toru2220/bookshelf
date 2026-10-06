FROM golang:1.24.3

ARG TARGETARCH
ENV TARGETARCH=${TARGETARCH}

RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    poppler-utils \
    ghostscript \
    curl \
    xz-utils \
    nginx \
    && rm -rf /var/lib/apt/lists/*

RUN set -eux; \
    case "${TARGETARCH}" in \
      amd64) FILE="7z2501-linux-x64.tar.xz" ;; \
      arm64) FILE="7z2501-linux-arm64.tar.xz" ;; \
      arm) FILE="7z2501-linux-arm.tar.xz" ;; \
      *) echo "Unsupported TARGETARCH: ${TARGETARCH}"; exit 1 ;; \
    esac; \
    URL="https://7-zip.org/a/${FILE}"; \
    echo "$URL"; \
    curl -fsSL "$URL" -o /tmp/7z.tar.xz; \
    mkdir -p /tmp/7zextract; \
    tar -xf /tmp/7z.tar.xz -C /tmp/7zextract; \
    install -m 755 /tmp/7zextract/7zz /usr/local/bin/7zz; \
    ln -sf /usr/local/bin/7zz /usr/local/bin/7z; \
    rm -rf /tmp/7z.tar.xz /tmp/7zextract

WORKDIR /back
COPY back/ .
RUN go mod tidy \
    && go build -o main .

COPY nginx.conf /etc/nginx/nginx.conf
COPY front/dist /front
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80
CMD ["/docker-entrypoint.sh"]
