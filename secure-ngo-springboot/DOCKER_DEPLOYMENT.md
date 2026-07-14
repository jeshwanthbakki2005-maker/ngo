# Docker Deployment Guide for Secure NGO Donation Tracking

## Prerequisites
- Docker installed on your system
- Docker Compose (included with Docker Desktop)

## Quick Start

### 1. Build and Run with Docker Compose (Recommended)

```bash
cd secure-ngo-springboot
docker-compose up --build
```

The application will be available at: **http://localhost:5000**

### 2. Build and Run with Docker Only

```bash
cd secure-ngo-springboot

# Build the image
docker build -t secure-ngo-donation .

# Run the container
docker run -d \
  --name secure-ngo-donation \
  -p 5000:5000 \
  -v ngo-data:/app/data \
  -e APP_FRONTEND_URL=http://localhost:5000 \
  secure-ngo-donation
```

### 3. Stop the Application

```bash
# With Docker Compose
docker-compose down

# With Docker only
docker stop secure-ngo-donation
docker rm secure-ngo-donation
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Application port | `5000` |
| `SPRING_DATASOURCE_URL` | H2 database path | `jdbc:h2:/app/data/charity_guard` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | (empty) |
| `JWT_SECRET` | JWT signing secret | (default key) |
| `JWT_EXPIRATION` | JWT token expiry (ms) | `86400000` (24h) |
| `APP_FRONTEND_URL` | Frontend URL for CORS | `http://localhost:5000` |
| `MAIL_ENABLED` | Enable email notifications | `false` |
| `MAIL_USERNAME` | Gmail username for SMTP | (empty) |
| `MAIL_PASSWORD` | Gmail app password | (empty) |

## Deploying to Cloud Platforms

### Deploy to Railway

1. Push your code to a GitHub repository
2. Go to [Railway](https://railway.app) and create a new project
3. Select "Deploy from GitHub repo"
4. Set the root directory to `secure-ngo-springboot`
5. Railway will automatically detect the Dockerfile
6. Add environment variables in Railway dashboard
7. Deploy!

### Deploy to Render

1. Push your code to a GitHub repository
2. Go to [Render](https://render.com) and create a new "Web Service"
3. Connect your GitHub repository
4. Set the root directory to `secure-ngo-springboot`
5. Render will detect the Dockerfile automatically
6. Set environment variables in Render dashboard
7. Deploy!

### Deploy to any VPS (DigitalOcean, AWS EC2, etc.)

```bash
# SSH into your server
ssh user@your-server-ip

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Clone your repository
git clone https://github.com/yourusername/your-repo.git
cd your-repo/secure-ngo-springboot

# Build and run
docker-compose up -d --build
```

## Data Persistence

The H2 database is stored in a Docker volume named `secure-ngo-donation-data`. This ensures your data persists even when the container is restarted or recreated.

To backup the database:
```bash
docker run --rm -v secure-ngo-donation-data:/data -v $(pwd):/backup alpine tar czf /backup/ngo-db-backup.tar.gz -C /data .
```

To restore from backup:
```bash
docker run --rm -v secure-ngo-donation-data:/data -v $(pwd):/backup alpine tar xzf /backup/ngo-db-backup.tar.gz -C /data
```

## Troubleshooting

### Check container logs
```bash
docker logs secure-ngo-donation
```

### Access container shell
```bash
docker exec -it secure-ngo-donation sh
```

### Rebuild from scratch
```bash
docker-compose down -v
docker-compose up --build
```

> **Note:** Vercel does NOT support Java/Spring Boot applications. Use Docker with Railway, Render, or a VPS instead.