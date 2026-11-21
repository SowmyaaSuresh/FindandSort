# Find & Sort — Java Swing

The game frontend and game logic are Java Swing. The browser only displays the
Swing desktop through Xvfb, x11vnc and noVNC.

## Startup behaviour
The application automatically starts a playable 2-player game. The player-count
dropdown remains in the top-right corner; choose 2–8 players and press Start Game
to restart with that number of players.

The Docker startup script verifies Xvfb, Java and VNC startup before opening the
web viewer. The viewer retries its connection automatically after transient
disconnects.

## Render deployment
Use the Docker runtime. Do not add manual environment variables; Render supplies
PORT automatically.
