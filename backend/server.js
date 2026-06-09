const express = require('express');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const multer = require('multer');
const fs = require('fs');
const path = require('path');
const https = require('https');
const AdmZip = require('adm-zip');
const sqlite3 = require('sqlite3').verbose();
const db = require('./database');

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

const JWT_SECRET = 'supersecret_anki_jwt_key_123';
const uploadDir = path.join(__dirname, 'uploads');
fs.mkdirSync(uploadDir, { recursive: true });
const upload = multer({ dest: uploadDir });
const TRANSLATION_WORD_LIMIT = 300;
const LOCAL_DICTIONARY_FILES = [
    '大学英语四级词汇.json',
    '大学英语六级词汇.json',
    '托福核心词汇.json',
    '雅思核心词汇.json'
];
const COMMON_TRANSLATIONS = {
    ability: 'n. 能力；本领',
    anymore: 'adv. 再也；不再',
    asked: 'v. 询问；请求',
    balloon: 'n. 气球',
    birthday: 'n. 生日',
    brought: 'v. 带来；拿来',
    cake: 'n. 蛋糕',
    clown: 'n. 小丑',
    dad: 'n. 爸爸',
    "didn't": 'aux. 没有；未曾',
    everyone: 'pron. 每个人；人人',
    fairy: 'n. 仙子；小精灵',
    forgotten: 'v. 忘记；遗忘',
    fun: 'n. 乐趣；快乐',
    games: 'n. 游戏；比赛',
    hide: 'v. 隐藏；躲藏',
    joined: 'v. 加入；参加',
    know: 'v. 知道；了解',
    mom: 'n. 妈妈',
    playing: 'v. 玩；参加；演奏',
    said: 'v. 说；讲',
    somebody: 'pron. 某人；有人',
    soon: 'adv. 很快；不久',
    time: 'n. 时间；时刻',
    wanted: 'v. 想要；需要',
    wonderful: 'adj. 精彩的；极好的',
    whose: 'pron. 谁的'
};

// Middleware for authentication
const authenticate = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader) return res.status(401).json({ error: 'No token provided' });

    const token = authHeader.split(' ')[1];
    jwt.verify(token, JWT_SECRET, (err, decoded) => {
        if (err) return res.status(401).json({ error: 'Invalid token' });
        req.user = decoded;
        next();
    });
};

// Middleware for admin check
const isAdmin = (req, res, next) => {
    if (req.user && req.user.role === 'admin') {
        next();
    } else {
        res.status(403).json({ error: 'Admin access required' });
    }
};

// =======================
// AUTH ROUTES
// =======================

// POST /api/auth/register
app.post('/api/auth/register', async (req, res) => {
    const { username, password, role } = req.body;
    if (!username || !password) return res.status(400).json({ error: 'Username and password required' });

    try {
        const hash = await bcrypt.hash(password, 10);
        const userRole = role === 'admin' ? 'admin' : 'user';
        
        db.run(`INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)`, [username, hash, userRole], function (err) {
            if (err) {
                if (err.message.includes('UNIQUE')) {
                    return res.status(409).json({ error: 'Username already exists' });
                }
                return res.status(500).json({ error: 'Database error' });
            }
            res.status(201).json({ message: 'User registered successfully', userId: this.lastID });
        });
    } catch (err) {
        res.status(500).json({ error: 'Server error' });
    }
});

// POST /api/auth/login
app.post('/api/auth/login', (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) return res.status(400).json({ error: 'Username and password required' });

    db.get(`SELECT * FROM users WHERE username = ?`, [username], async (err, user) => {
        if (err) return res.status(500).json({ error: 'Database error' });
        if (!user) return res.status(401).json({ error: 'Invalid credentials' });

        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch) return res.status(401).json({ error: 'Invalid credentials' });

        const token = jwt.sign({ id: user.id, username: user.username, role: user.role }, JWT_SECRET, { expiresIn: '7d' });
        res.json({ message: 'Login successful', token, user: { id: user.id, username: user.username, role: user.role } });
    });
});

// =======================
// DECKS ROUTES
// =======================

// GET /api/decks
app.get('/api/decks', authenticate, (req, res) => {
    db.all(`SELECT * FROM decks WHERE user_id = ?`, [req.user.id], (err, rows) => {
        if (err) return res.status(500).json({ error: 'Database error' });
        res.json(rows);
    });
});

// 简单的 HTML 清理函数
function cleanHtml(str) {
    if (!str) return '';
    return str.replace(/<br\s*\/?>/gi, '\n')
              .replace(/<[^>]*>?/gm, '')
              .replace(/&nbsp;/g, ' ')
              .trim();
}

function safeUnlink(filePath) {
    if (!filePath) return;
    try {
        if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
    } catch (_error) {
        // Temporary upload cleanup should not mask the real import result.
    }
}

function normalizeWord(word) {
    return (word || '')
        .toLowerCase()
        .replace(/[’‘`]/g, "'")
        .replace(/^[^a-z]+|[^a-z'-]+$/gi, '')
        .replace(/'s$/i, '')
        .replace(/s'$/i, 's')
        .trim();
}

function loadLocalTranslationDictionary() {
    const dictionary = new Map(Object.entries(COMMON_TRANSLATIONS));

    LOCAL_DICTIONARY_FILES.forEach((fileName) => {
        const filePath = path.join(__dirname, '..', fileName);
        try {
            const fileContent = fs.readFileSync(filePath, 'utf8');
            const deck = JSON.parse(fileContent);
            if (!Array.isArray(deck.cards)) return;

            deck.cards.forEach((card) => {
                const word = normalizeWord(card.front || card.front_content);
                const translation = cleanHtml(card.back || card.back_content);
                if (word && translation && !dictionary.has(word)) {
                    dictionary.set(word, translation);
                }
            });
        } catch (_error) {
            // Missing optional local dictionaries should not block translation.
        }
    });

    return dictionary;
}

const LOCAL_TRANSLATION_DICTIONARY = loadLocalTranslationDictionary();

function getLocalTranslation(word) {
    return LOCAL_TRANSLATION_DICTIONARY.get(normalizeWord(word)) || '';
}

function parseImportedJsonDeck(fileContent, originalName) {
    const data = JSON.parse(fileContent.replace(/^\uFEFF/, ''));
    const sourceCards = Array.isArray(data)
        ? data
        : Array.isArray(data.cards)
            ? data.cards
            : Array.isArray(data.words)
                ? data.words
                : [];

    const cards = sourceCards
        .map((card) => {
            if (Array.isArray(card)) {
                return {
                    front: cleanHtml(card[0]),
                    back: cleanHtml(card[1]),
                    partOfSpeech: cleanHtml(card[2] || '') || null
                };
            }

            if (!card || typeof card !== 'object') return null;

            const front = cleanHtml(
                card.front ||
                card.front_content ||
                card.word ||
                card.term ||
                card.question ||
                ''
            );
            const back = cleanHtml(
                card.back ||
                card.back_content ||
                card.translation ||
                card.meaning ||
                card.definition ||
                card.answer ||
                ''
            );
            const partOfSpeech = cleanHtml(card.partOfSpeech || card.part_of_speech || card.pos || '') || null;

            return { front, back, partOfSpeech };
        })
        .filter(card => card && card.front && card.back);

    const fallbackName = originalName
        ? path.basename(originalName, path.extname(originalName))
        : 'Imported Deck';
    const name = !Array.isArray(data)
        ? (data.name || data.deckName || data.title || fallbackName)
        : fallbackName;
    const description = !Array.isArray(data)
        ? (data.description || data.desc || '')
        : '';

    return { name, description, cards };
}

function fetchJson(url) {
    return new Promise((resolve, reject) => {
        const req = https.get(url, {
            headers: {
                'Accept': 'application/json',
                'User-Agent': 'Mozilla/5.0'
            }
        }, (response) => {
            let rawData = '';

            response.on('data', (chunk) => {
                rawData += chunk;
            });

            response.on('end', () => {
                if (response.statusCode < 200 || response.statusCode >= 300) {
                    return reject(new Error(`Translation request failed with status ${response.statusCode}`));
                }

                try {
                    resolve(JSON.parse(rawData));
                } catch (_err) {
                    reject(new Error('Invalid translation response'));
                }
            });
        });

        req.on('error', reject);
        req.setTimeout(5000, () => {
            req.destroy(new Error('Translation request timed out'));
        });
    });
}

async function translateWithGoogle(word) {
    const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=zh-CN&dt=t&q=${encodeURIComponent(word)}`;
    const payload = await fetchJson(url);
    const translatedText = Array.isArray(payload?.[0])
        ? payload[0]
            .map(item => Array.isArray(item) ? item[0] : '')
            .join('')
            .trim()
        : '';

    if (!translatedText) {
        throw new Error(`No translation returned for ${word}`);
    }

    return translatedText;
}

async function translateWithMyMemory(word) {
    const url = `https://api.mymemory.translated.net/get?q=${encodeURIComponent(word)}&langpair=en%7Czh-CN`;
    const payload = await fetchJson(url);
    const translatedText = (payload?.responseData?.translatedText || '').trim();

    if (!translatedText || translatedText.toLowerCase() === word.toLowerCase()) {
        throw new Error(`No translation returned for ${word}`);
    }

    return translatedText;
}

async function translateWordToChinese(word) {
    const localTranslation = getLocalTranslation(word);
    if (localTranslation) {
        return localTranslation;
    }

    try {
        return await translateWithMyMemory(word);
    } catch (_myMemoryError) {
        return translateWithGoogle(word);
    }
}

async function translateWordsToChinese(words) {
    return Promise.all(words.map(async (word) => {
        try {
            const translation = await translateWordToChinese(word);
            return { word, translation };
        } catch (_error) {
            return { word, translation: '' };
        }
    }));
}

async function translateWordsHandler(req, res) {
    const inputWords = Array.isArray(req.body?.words) ? req.body.words : [];
    const words = [...new Set(
        inputWords
            .map(normalizeWord)
            .filter(word => /^[a-z]+(?:['-][a-z]+)*$/i.test(word))
    )];

    if (words.length === 0) {
        return res.status(400).json({ error: 'Words array is required' });
    }

    if (words.length > TRANSLATION_WORD_LIMIT) {
        return res.status(400).json({ error: `Too many words in one request, max ${TRANSLATION_WORD_LIMIT}` });
    }

    try {
        const translations = await translateWordsToChinese(words);
        res.json({ translations });
    } catch (_error) {
        res.status(502).json({ error: 'Translation service unavailable' });
    }
}

// POST /api/decks (Create or import)
app.post('/api/decks', authenticate, upload.single('file'), (req, res) => {
    // If a file is uploaded, handle import
    if (req.file) {
        const isApkg = req.file.originalname && req.file.originalname.toLowerCase().endsWith('.apkg');
        
        if (isApkg) {
            const deckName = path.basename(req.file.originalname, '.apkg');
            const tempDir = path.join(__dirname, 'temp_anki_extract_' + Date.now());
            
            try {
                const zip = new AdmZip(req.file.path);
                zip.extractAllTo(tempDir, true);

                let dbPath = path.join(tempDir, 'collection.anki21');
                if (!fs.existsSync(dbPath)) {
                    dbPath = path.join(tempDir, 'collection.anki2');
                }

                if (!fs.existsSync(dbPath)) {
                    // It might be nested inside a folder
                    const files = fs.readdirSync(tempDir);
                    for (const file of files) {
                        const nestedPath = path.join(tempDir, file, 'collection.anki21');
                        const nestedPath2 = path.join(tempDir, file, 'collection.anki2');
                        if (fs.existsSync(nestedPath)) {
                            dbPath = nestedPath;
                            break;
                        } else if (fs.existsSync(nestedPath2)) {
                            dbPath = nestedPath2;
                            break;
                        }
                    }
                }

                if (!fs.existsSync(dbPath)) {
                    fs.rmSync(tempDir, { recursive: true, force: true });
                    fs.unlinkSync(req.file.path);
                    return res.status(400).json({ error: 'Not a valid Anki apkg file' });
                }

                const ankiDb = new sqlite3.Database(dbPath, sqlite3.OPEN_READONLY, (err) => {
                    if (err) {
                        fs.rmSync(tempDir, { recursive: true, force: true });
                        fs.unlinkSync(req.file.path);
                        return res.status(500).json({ error: 'Failed to open Anki database' });
                    }
                });

                ankiDb.all(`SELECT flds FROM notes`, [], (err, rows) => {
                    if (err) {
                        ankiDb.close();
                        fs.rmSync(tempDir, { recursive: true, force: true });
                        fs.unlinkSync(req.file.path);
                        return res.status(500).json({ error: 'Failed to query Anki database' });
                    }

                    const cards = [];
                    rows.forEach(row => {
                        if (row.flds) {
                            const fields = row.flds.split('\x1f');
                            if (fields.length >= 2) {
                                const cleanFront = cleanHtml(fields[0]);
                                const cleanBack = cleanHtml(fields[1]);
                                if (cleanFront && cleanBack) {
                                    cards.push({ front: cleanFront, back: cleanBack });
                                }
                            }
                        }
                    });

                    db.run(`INSERT INTO decks (user_id, name, description) VALUES (?, ?, ?)`, [req.user.id, deckName, "Imported from Anki"], function (err) {
                        if (err) {
                            ankiDb.close();
                            fs.rmSync(tempDir, { recursive: true, force: true });
                            fs.unlinkSync(req.file.path);
                            return res.status(500).json({ error: 'Database error creating deck' });
                        }
                        
                        const deckId = this.lastID;
                        const stmt = db.prepare(`INSERT INTO cards (deck_id, front_content, back_content) VALUES (?, ?, ?)`);
                        cards.forEach(card => {
                            stmt.run(deckId, card.front, card.back);
                        });
                        stmt.finalize();

                        ankiDb.close(() => {
                            fs.rmSync(tempDir, { recursive: true, force: true });
                            fs.unlinkSync(req.file.path);
                        });
                        
                        res.status(201).json({ message: 'Apkg imported successfully', deckId });
                    });
                });
            } catch (err) {
                fs.unlinkSync(req.file.path);
                if (fs.existsSync(tempDir)) fs.rmSync(tempDir, { recursive: true, force: true });
                res.status(500).json({ error: 'Error processing apkg file' });
            }
        } else {
            try {
                const fileContent = fs.readFileSync(req.file.path, 'utf8');
                const { name, description, cards } = parseImportedJsonDeck(fileContent, req.file.originalname);

                if (cards.length === 0) {
                    safeUnlink(req.file.path);
                    return res.status(400).json({
                        error: 'JSON file must contain cards with front/back, word/translation, or question/answer fields'
                    });
                }

                db.run(`INSERT INTO decks (user_id, name, description) VALUES (?, ?, ?)`, [req.user.id, name, description], function (err) {
                    if (err) {
                        safeUnlink(req.file.path);
                        return res.status(500).json({ error: 'Database error creating deck' });
                    }
                    const deckId = this.lastID;
                    
                    const stmt = db.prepare(`INSERT INTO cards (deck_id, front_content, back_content, part_of_speech) VALUES (?, ?, ?, ?)`);
                    cards.forEach(card => {
                        stmt.run(deckId, card.front, card.back, card.partOfSpeech || null);
                    });
                    stmt.finalize((err) => {
                        safeUnlink(req.file.path);
                        if (err) return res.status(500).json({ error: 'Database error importing cards' });
                        res.status(201).json({ message: 'Deck imported successfully', deckId, importedCards: cards.length });
                    });
                });
            } catch (error) {
                safeUnlink(req.file.path);
                res.status(400).json({ error: `Invalid JSON file format: ${error.message}` });
            }
        }
    } else {
        // Normal creation
        const { name, description } = req.body;
        if (!name) return res.status(400).json({ error: 'Deck name is required' });

        db.run(`INSERT INTO decks (user_id, name, description) VALUES (?, ?, ?)`, [req.user.id, name, description], function (err) {
            if (err) return res.status(500).json({ error: 'Database error' });
            res.status(201).json({ message: 'Deck created successfully', deckId: this.lastID });
        });
    }
});

// POST /api/translate/words
app.post([
    '/api/translate/words',
    '/api/words/translate',
    '/api/translate',
    '/translate/words'
], authenticate, translateWordsHandler);

// POST /api/decks/:id/cards (Add cards to an existing deck)
app.post('/api/decks/:id/cards', authenticate, (req, res) => {
    const deckId = req.params.id;
    const { cards } = req.body;
    
    if (!cards || !Array.isArray(cards)) return res.status(400).json({ error: 'Cards array is required' });

    db.get(`SELECT * FROM decks WHERE id = ? AND user_id = ?`, [deckId, req.user.id], (err, deck) => {
        if (err) return res.status(500).json({ error: 'Database error' });
        if (!deck) return res.status(404).json({ error: 'Deck not found or unauthorized' });

        const stmt = db.prepare(`INSERT INTO cards (deck_id, front_content, back_content, part_of_speech) VALUES (?, ?, ?, ?)`);
        cards.forEach(card => {
            stmt.run(deckId, card.front || card.front_content, card.back || card.back_content, card.partOfSpeech || card.part_of_speech || null);
        });
        stmt.finalize((err) => {
            if (err) return res.status(500).json({ error: 'Database error adding cards' });
            res.status(201).json({ message: 'Cards added successfully' });
        });
    });
});

// GET /api/decks/:id/cards (Cards to study)
app.get('/api/decks/:id/cards', authenticate, (req, res) => {
    const deckId = req.params.id;
    // Check if deck belongs to user
    db.get(`SELECT * FROM decks WHERE id = ? AND user_id = ?`, [deckId, req.user.id], (err, deck) => {
        if (err) return res.status(500).json({ error: 'Database error' });
        if (!deck) return res.status(404).json({ error: 'Deck not found or unauthorized' });

        // Get cards that are new, or review time is due
        const now = new Date().toISOString();
        db.all(`SELECT * FROM cards WHERE deck_id = ? AND (status = 'new' OR next_review <= ?)`, [deckId, now], (err, cards) => {
            if (err) return res.status(500).json({ error: 'Database error' });
            res.json(cards);
        });
    });
});

// =======================
// CARDS ROUTES
// =======================

// POST /api/cards/:id/review
app.post('/api/cards/:id/review', authenticate, (req, res) => {
    const cardId = req.params.id;
    const { performanceRating } = req.body; // 0-3 rating for example (0=Again, 1=Hard, 2=Good, 3=Easy)

    // A very simple spaced repetition update logic
    db.get(`SELECT * FROM cards WHERE id = ?`, [cardId], (err, card) => {
        if (err) return res.status(500).json({ error: 'Database error' });
        if (!card) return res.status(404).json({ error: 'Card not found' });

        let { status, interval, ease_factor } = card;
        
        // Basic SM-2 inspired logic
        if (performanceRating === undefined || performanceRating < 0 || performanceRating > 3) {
            return res.status(400).json({ error: 'Valid performanceRating (0-3) is required' });
        }

        if (performanceRating === 0) {
            status = 'learning';
            interval = 1; // 1 day or reset
            ease_factor = Math.max(130, ease_factor - 20);
        } else {
            status = 'review';
            if (interval === 0) {
                interval = 1;
            } else if (interval === 1) {
                interval = 6;
            } else {
                interval = Math.round(interval * (ease_factor / 100));
            }
            if (performanceRating === 2) {
                ease_factor = ease_factor;
            } else if (performanceRating === 3) {
                ease_factor += 15;
            } else if (performanceRating === 1) {
                ease_factor = Math.max(130, ease_factor - 15);
            }
        }

        const next_review = new Date();
        next_review.setDate(next_review.getDate() + interval);
        const nextReviewIso = next_review.toISOString();

        db.run(
            `UPDATE cards SET status = ?, interval = ?, ease_factor = ?, next_review = ? WHERE id = ?`,
            [status, interval, ease_factor, nextReviewIso, cardId],
            function (err) {
                if (err) return res.status(500).json({ error: 'Database update error' });
                
                // Record the review log for statistics
                db.run(
                    `INSERT INTO review_logs (card_id, user_id, rating) VALUES (?, ?, ?)`,
                    [cardId, req.user.id, performanceRating],
                    (errLog) => {
                        if (errLog) console.error('Failed to insert review log:', errLog);
                        res.json({ message: 'Card review recorded', next_review: nextReviewIso });
                    }
                );
            }
        );
    });
});

// =======================
// STATS ROUTES
// =======================

// GET /api/stats
app.get('/api/stats', authenticate, (req, res) => {
    // 1. Proficiency stats (count of cards by status)
    db.all(`
        SELECT status, COUNT(*) as count 
        FROM cards 
        JOIN decks ON cards.deck_id = decks.id 
        WHERE decks.user_id = ? 
        GROUP BY status
    `, [req.user.id], (err, proficiencyRows) => {
        if (err) return res.status(500).json({ error: 'Database error fetching proficiency' });

        // 2. Review frequency (count of reviews per day for the last 7 days)
        db.all(`
            SELECT date(review_date) as date, COUNT(*) as count 
            FROM review_logs 
            WHERE user_id = ? 
            GROUP BY date(review_date) 
            ORDER BY date(review_date) DESC 
            LIMIT 7
        `, [req.user.id], (err, frequencyRows) => {
            if (err) return res.status(500).json({ error: 'Database error fetching frequency' });
            
            res.json({
                proficiency: proficiencyRows,
                frequency: frequencyRows
            });
        });
    });
});

// =======================
// ADMIN ROUTES
// =======================

// GET /api/admin/users
app.get('/api/admin/users', authenticate, isAdmin, (req, res) => {
    db.all(`SELECT id, username, role FROM users`, [], (err, rows) => {
        if (err) return res.status(500).json({ error: 'Database error' });
        res.json(rows);
    });
});

// POST /api/admin/users
app.post('/api/admin/users', authenticate, isAdmin, async (req, res) => {
    const { username, password, role } = req.body;
    if (!username || !password) return res.status(400).json({ error: 'Username and password required' });
    try {
        const hash = await bcrypt.hash(password, 10);
        const userRole = role === 'admin' ? 'admin' : 'user';
        db.run(`INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)`, [username, hash, userRole], function (err) {
            if (err) {
                if (err.message.includes('UNIQUE')) return res.status(409).json({ error: 'Username already exists' });
                return res.status(500).json({ error: 'Database error' });
            }
            res.status(201).json({ message: 'User created successfully', id: this.lastID });
        });
    } catch (err) {
        res.status(500).json({ error: 'Server error' });
    }
});

// PUT /api/admin/users/:id
app.put('/api/admin/users/:id', authenticate, isAdmin, async (req, res) => {
    const userId = req.params.id;
    const { username, password, role } = req.body;
    if (!username) return res.status(400).json({ error: 'Username required' });
    
    // Check if target user is admin
    db.get(`SELECT role FROM users WHERE id = ?`, [userId], async (err, row) => {
        if (err) return res.status(500).json({ error: 'Database error' });
        if (!row) return res.status(404).json({ error: 'User not found' });
        
        // 检查目标用户的角色，如果是管理员则拒绝互相编辑 (自己可以编辑自己)
        if (row.role === 'admin' && parseInt(req.user.id) !== parseInt(userId)) {
            return res.status(403).json({ error: '安全限制：管理员之间不可以互相编辑账号！' });
        }

        try {
            const userRole = role === 'admin' ? 'admin' : 'user';
            if (password) {
                const hash = await bcrypt.hash(password, 10);
                db.run(`UPDATE users SET username = ?, password_hash = ?, role = ? WHERE id = ?`, [username, hash, userRole, userId], function(err) {
                    if (err) return res.status(500).json({ error: 'Database error' });
                    res.json({ message: 'User updated successfully' });
                });
            } else {
                db.run(`UPDATE users SET username = ?, role = ? WHERE id = ?`, [username, userRole, userId], function(err) {
                    if (err) return res.status(500).json({ error: 'Database error' });
                    res.json({ message: 'User updated successfully' });
                });
            }
        } catch (err) {
            res.status(500).json({ error: 'Server error' });
        }
    });
});

// DELETE /api/admin/users/:id
app.delete('/api/admin/users/:id', authenticate, isAdmin, (req, res) => {
    const userId = req.params.id;
    db.get(`SELECT role FROM users WHERE id = ?`, [userId], (err, row) => {
        if (err) return res.status(500).json({ error: 'Database error' });
        if (!row) return res.status(404).json({ error: 'User not found' });
        
        // 检查目标用户的角色，如果是管理员则拒绝删除
        if (row.role === 'admin') {
            return res.status(403).json({ error: '安全限制：管理员之间不可以互相删除账号！' });
        }
        
        db.run(`DELETE FROM users WHERE id = ?`, [userId], function(err) {
            if (err) return res.status(500).json({ error: 'Database error' });
            // Cascade delete related data
            db.run(`DELETE FROM decks WHERE user_id = ?`, [userId], (err) => {
                if (!err) {
                    // Not ideal for cascading cards but keeps it simple for now
                }
            });
            db.run(`DELETE FROM review_logs WHERE user_id = ?`, [userId]);
            res.json({ message: 'User deleted successfully' });
        });
    });
});

// GET /api/admin/users/:id/decks
app.get('/api/admin/users/:id/decks', authenticate, isAdmin, (req, res) => {
    db.all(`SELECT * FROM decks WHERE user_id = ?`, [req.params.id], (err, rows) => {
        if (err) return res.status(500).json({ error: 'Database error' });
        res.json(rows);
    });
});

// GET /api/admin/users/:id/stats
app.get('/api/admin/users/:id/stats', authenticate, isAdmin, (req, res) => {
    const userId = req.params.id;
    db.all(`
        SELECT status, COUNT(*) as count 
        FROM cards 
        JOIN decks ON cards.deck_id = decks.id 
        WHERE decks.user_id = ? 
        GROUP BY status
    `, [userId], (err, proficiencyRows) => {
        if (err) return res.status(500).json({ error: 'Database error fetching proficiency' });

        db.all(`
            SELECT date(review_date) as date, COUNT(*) as count 
            FROM review_logs 
            WHERE user_id = ? 
            GROUP BY date(review_date) 
            ORDER BY date(review_date) DESC 
            LIMIT 7
        `, [userId], (err, frequencyRows) => {
            if (err) return res.status(500).json({ error: 'Database error fetching frequency' });
            
            res.json({
                proficiency: proficiencyRows,
                frequency: frequencyRows
            });
        });
    });
});

// Fallback route to serve index.html for any unknown GET request (SPA support)
app.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
