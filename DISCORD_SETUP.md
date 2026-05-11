# Discord Integration Setup

This guide will help you set up Discord integration to receive essays directly in your DM.

## Option 1: Using Discord Webhook (Easiest)

### Step 1: Create a Discord Bot

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
2. Click **New Application** and give it a name (e.g., "Daily Book Bot")
3. Go to the **Bot** section in the left sidebar
4. Click **Reset Token** and copy the bot token
5. Under **Privileged Gateway Intents**, enable **MESSAGE CONTENT INTENT**

### Step 2: Get Your Discord User ID

1. In Discord, enable Developer Mode:
   - User Settings → Advanced → Developer Mode (toggle on)
2. Right-click your username and select **Copy User ID**

### Step 3: Create a Webhook (Alternative to Bot)

1. Create a new server (or use an existing one)
2. Go to Server Settings → Integrations → Webhooks
3. Click **New Webhook**
4. Give it a name and copy the Webhook URL

### Step 4: Configure Environment Variables

Edit your `.env` file:

```bash
# Using Webhook
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN

# OR using Bot + Direct Message
DISCORD_USER_ID=your-discord-user-id
DISCORD_BOT_TOKEN=your-bot-token
```

### Step 5: Invite Bot to Server (if using Bot)

1. Go to **OAuth2 → URL Generator** in the Developer Portal
2. Select scopes: `bot`, `applications.commands`
3. Select permissions: `Send Messages`, `Embed Links`
4. Copy the generated URL and open it in your browser
5. Select your server and authorize

## Option 2: Using Direct Message (Requires Bot)

For Direct Messages, you need to:

1. Follow Step 1 above to create a bot
2. Follow Step 2 above to get your User ID
3. Add the bot to a server where you have permission
4. Send a message to the bot first (to start the conversation)
5. The bot can then send you DMs

## Testing the Integration

Once configured, test by calling the API:

```bash
# Generate essay and send to Discord
curl -X POST http://localhost:8080/api/books/essay/discord
```

Or run the application and visit:
- `http://localhost:8080/api/books/essay/discord` in your browser

## Example Response

```json
{
  "status": "success",
  "message": "Essay generated and sent to Discord DM",
  "essay": {
    "id": "essay_1234567890",
    "selectedBook": {
      "title": "The AI Revolution",
      "author": "John Smith",
      "category": "ai"
    },
    "title": "A 10-minute Essay: The AI Revolution",
    "appleMusicLink": {
      "url": "https://music.apple.com/us/playlist/study/playlists/...",
      "deepLink": "music://...",
      "formattedText": "Listen to study music while reading: ..."
    }
  }
}
```

## Troubleshooting

### "Discord DM credentials not configured"
- Make sure `DISCORD_USER_ID` and `DISCORD_BOT_TOKEN` are set in your `.env` file
- Verify the bot has MESSAGE CONTENT INTENT enabled

### "Error sending to Discord: ..."
- Check if the webhook URL or bot token is correct
- Verify the bot has permission to send messages in the channel/server

### "Failed to get DM channel"
- Make sure you've sent a message to the bot first
- The bot needs an existing conversation to send DMs
