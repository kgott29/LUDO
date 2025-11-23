# LUDO
A complete, fully playable Ludo board game built using Java Swing, featuring animations, dice rolling, token movement, capturing logic, safe zones, and winner detection.
The game supports 4 players (Red, Green, Yellow, Blue) and follows traditional Ludo rules.

**Features:**


1. Dice Rolling

Smooth animated dice roll using javax.swing.Timer

Highlights current player's dice in their color

Auto-handles conditions for extra turns (6 rolled, capture, reach-home)

2. Game Board

15×15 classic Ludo layout

Safe spots marked with gold stars

Home areas colored per player

Smooth token stacking (if multiple tokens overlap)

3. Token Mechanics

Tokens animate and highlight when selectable

Rules implemented:

Token enters path only on rolling 6

Tokens move according to dice value

Capturing opponent tokens

Safe-zone protection

Reaching final home square

Win condition when all 4 tokens reach home

4. Graphics & UI

Modern colors with rounded markers

Center triangles like original Ludo

**Rules Implemented:**


4 tokens per player

Must roll 6 to activate a token from home

Landing on enemy → capture

Landing on safe spot → no capture

Rolling 6, capturing, or reaching home → extra turn

All 4 tokens home → player wins

**Technologies Used:**


Java 8+

Swing (JFrame, JPanel, Timer)

AWT Graphics2D

Shadow and highlight effects

Responsive messages indicating game flow
