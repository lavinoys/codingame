# <https://www.codingame.com/multiplayer/optimization/wordle>

## Learning Opportunities

This puzzle can be solved using the following concepts. Practice using these concepts and improve your skills.

Brute-force
Combinatorics

```text
Statement
The Goal
This is an adaptation of an online game Wordle (https://www.nytimes.com/games/wordle/index.html) with slight modifications in the rules.

The goal of the game is to guess the hidden word based on the hints given each turn.
Rules
At each turn, the player will guess a word.
After each guess, the player will know the state of each letter.
Letter at a position can have four states:
State 1 means the letter at the position is not present in the word.
State 2 means the letter at the position is present in the word but position is incorrect.
State 3 means the letter at the position is present in the word and position is correct.
State 0 means unknown. For the first turn only.
A letter can appear multiple times in the word.
Same letter twice or multiple times in the word will work as follows:
If ORBIT is the word and player guessed ABBEY, B at the 2nd position has state 2 and B at the 3rd position has state 3.
If CELEB is the word and player guessed ABBEY, B at both the 2nd and 3rd position have state 2.
If SHARP is the word and player guessed ABBEY, B at both the 2nd and 3rd position have state 1.
Leaderboard is ascended by the amount of total guesses among all the validation test cases.
Victory Conditions
You guess the word in less than 27 turns.
Defeat Conditions
You use more than 26 guesses.
You make a guess which is too short or too long.
Your guess containing characters other than alphabetical letters.
Note
It is allowed to make guesses with unreal words although the word the player have to guess is a real word.
```
## The Goal

This is an adaptation of an online game Wordle (https://www.nytimes.com/games/wordle/index.html) with slight modifications in the rules.

The goal of the game is to guess the hidden word based on the hints given each turn.

## Rules

At each turn, the player will guess a word.
After each guess, the player will know the state of each letter.
Letter at a position can have four states:
State 1 means the letter at the position is not present in the word.
State 2 means the letter at the position is present in the word but position is incorrect.
State 3 means the letter at the position is present in the word and position is correct.
State 0 means unknown. For the first turn only.
A letter can appear multiple times in the word.
Same letter twice or multiple times in the word will work as follows:
If ORBIT is the word and player guessed ABBEY, B at the 2nd position has state 2 and B at the 3rd position has state 3.
If CELEB is the word and player guessed ABBEY, B at both the 2nd and 3rd position have state 2.
If SHARP is the word and player guessed ABBEY, B at both the 2nd and 3rd position have state 1.
Leaderboard is ascended by the amount of total guesses among all the validation test cases.

### Victory Conditions

You guess the word in less than 27 turns.

### Defeat Conditions

You use more than 26 guesses.
You make a guess which is too short or too long.
Your guess containing characters other than alphabetical letters.

## Note

It is allowed to make guesses with unreal words although the word the player have to guess is a real word.

## Game Input

The program must first read the initialization data from standard input. Then, provide to the standard output one line for each game turn.

### Initializaton input

Line 1: An integer wordCount, number of words in the word set.

Line 2: wordCount amount of space separated words, each containing exactly 6 letters. Represents the word set, the word the player have to guess belongs to the word set.

### Input for a game turn

A single line: 6 space separated integers, each representing the state of the letter of the corresponding position of previous guess.

For the first turn all the 6 states are 0.
Output for a game turn
A single line containing a word of length 6 letters in uppercase.

## Constraints

wordCount ≈ 10000
Word length = 6 letters
Response time per turn ≤ 50 ms
Response time for the first turn ≤ 1000 ms
Number of turns ≤ 26