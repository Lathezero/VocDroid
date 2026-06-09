const sqlite3 = require('sqlite3').verbose();
const path = require('path');

const dbPath = path.resolve(__dirname, 'data.db');

const db = new sqlite3.Database(dbPath, (err) => {
    if (err) {
        console.error('Error connecting to database:', err.message);
    } else {
        console.log('Connected to SQLite database.');
        initDb();
    }
});

function initDb() {
    db.serialize(() => {
        // Create users table
        db.run(`
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                role TEXT DEFAULT 'user'
            )
        `);

        // Create decks table
        db.run(`
            CREATE TABLE IF NOT EXISTS decks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        `);

        // Create cards table
        db.run(`
            CREATE TABLE IF NOT EXISTS cards (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                deck_id INTEGER NOT NULL,
                front_content TEXT NOT NULL,
                back_content TEXT NOT NULL,
                part_of_speech TEXT,
                status TEXT DEFAULT 'new',
                next_review DATETIME,
                interval INTEGER DEFAULT 0,
                ease_factor INTEGER DEFAULT 250,
                FOREIGN KEY(deck_id) REFERENCES decks(id)
            )
        `);

        // Safely add part_of_speech to existing table if it doesn't exist
        db.run(`ALTER TABLE cards ADD COLUMN part_of_speech TEXT`, (err) => {
            // Ignore error if column already exists
        });

        // Create review logs table for statistics
        db.run(`
            CREATE TABLE IF NOT EXISTS review_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                card_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                rating INTEGER NOT NULL,
                review_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(card_id) REFERENCES cards(id),
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        `);
        
        console.log('Database tables initialized.');
    });
}

module.exports = db;
