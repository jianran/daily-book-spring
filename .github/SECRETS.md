# GitHub Secrets Configuration

This document describes the secrets required for the CI/CD pipeline.

## Required Secrets

### Repository Secrets (Settings > Secrets and variables > Actions)

| Secret Name | Description | Where to Get |
|-------------|-------------|--------------|
| `DEEPSEEK_API_KEY` | Your DeepSeek AI API key | [deepseek.com](https://deepseek.com) |
| `DEPLOY_HOST` | Server IP or hostname for deployment | Your hosting provider |
| `DEPLOY_USER` | SSH username for server access | Your hosting provider |
| `DEPLOY_KEY` | SSH private key for server access | Generate with `ssh-keygen` |

## Adding Secrets to GitHub

1. Go to your repository on GitHub
2. Navigate to **Settings** > **Secrets and variables** > **Actions**
3. Click **New repository secret**
4. Enter the secret name and value
5. Click **Add secret**

## Local Development Setup

For local development, create a `.env` file in the project root:

```bash
cp .env.example .env
nano .env  # or your preferred editor
```

Add your DeepSeek API key:
```
DEEPSEEK_API_KEY=sk-your-real-api-key-here
```

## Testing Without API Key

The application will use a fallback essay generation when the API key is missing or invalid. This is useful for:
- Testing the API structure
- Developing client integrations
- Running without costs during development

To test the fallback mode, either:
1. Leave `DEEPSEEK_API_KEY` empty in `.env`
2. Set it to any placeholder value like `test-key`
