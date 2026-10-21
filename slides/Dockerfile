# The talk's toolchain: Node, the packages of the lockfile, and the Chromium the export prints with. The tag is the
# playwright-chromium version of package.json, so the package finds its browser in the image.
FROM mcr.microsoft.com/playwright:v1.64.1-noble
WORKDIR /slides
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
ENTRYPOINT ["npx", "--no-install", "slidev"]
