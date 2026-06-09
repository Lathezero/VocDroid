const fs = require('fs');
const path = require('path');
const AdmZip = require('adm-zip');
const sqlite3 = require('sqlite3').verbose();

// 简单的 HTML 清理函数：将 <br> 转为换行，去除其他标签
function cleanHtml(str) {
    if (!str) return '';
    return str.replace(/<br\s*\/?>/gi, '\n') // 替换换行符
              .replace(/<[^>]*>?/gm, '')     // 去除 HTML 标签
              .replace(/&nbsp;/g, ' ')       // 替换空格实体
              .trim();
}

function convertApkg(inputFilePath) {
    if (!inputFilePath || !inputFilePath.endsWith('.apkg')) {
        console.error('用法: node convert.js <你的牌组文件.apkg>');
        process.exit(1);
    }

    if (!fs.existsSync(inputFilePath)) {
        console.error(`找不到文件: ${inputFilePath}`);
        process.exit(1);
    }

    const deckName = path.basename(inputFilePath, '.apkg');
    const outputJsonPath = path.join(__dirname, `${deckName}.json`);
    const tempDir = path.join(__dirname, 'temp_anki_extract_' + Date.now());

    try {
        console.log(`正在解压 ${inputFilePath}...`);
        const zip = new AdmZip(inputFilePath);
        zip.extractAllTo(tempDir, true);

        // Anki 的数据库文件可能是 collection.anki2 或 collection.anki21
        let dbPath = path.join(tempDir, 'collection.anki21');
        if (!fs.existsSync(dbPath)) {
            dbPath = path.join(tempDir, 'collection.anki2');
        }

        if (!fs.existsSync(dbPath)) {
            console.error('解压失败或不是有效的 Anki 牌组文件（找不到数据库）。');
            fs.rmSync(tempDir, { recursive: true, force: true });
            process.exit(1);
        }

        console.log('正在读取 Anki 数据库解析卡片...');
        const db = new sqlite3.Database(dbPath, sqlite3.OPEN_READONLY, (err) => {
            if (err) {
                console.error('打开数据库失败:', err.message);
                fs.rmSync(tempDir, { recursive: true, force: true });
                process.exit(1);
            }
        });

        // 读取 notes 表中的 flds 字段（里面存放了正反面内容）
        db.all(`SELECT flds FROM notes`, [], (err, rows) => {
            if (err) {
                console.error('查询数据库失败:', err);
                db.close();
                fs.rmSync(tempDir, { recursive: true, force: true });
                process.exit(1);
            }

            const cards = [];
            rows.forEach(row => {
                if (row.flds) {
                    // Anki 字段之间默认使用分隔符 \x1f 隔开
                    const fields = row.flds.split('\x1f');
                    if (fields.length >= 2) {
                        const cleanFront = cleanHtml(fields[0]);
                        const cleanBack = cleanHtml(fields[1]);
                        
                        // 确保正反面都有内容
                        if (cleanFront && cleanBack) {
                            cards.push({
                                front: cleanFront,
                                back: cleanBack
                            });
                        }
                    }
                }
            });

            const outputData = {
                name: deckName,
                description: "从 Anki 导入的牌组",
                cards: cards
            };

            fs.writeFileSync(outputJsonPath, JSON.stringify(outputData, null, 2), 'utf-8');
            console.log(`\n✅ 转换成功！`);
            console.log(`共提取了 ${cards.length} 张卡片。`);
            console.log(`文件已保存为: ${outputJsonPath}`);

            // 清理临时文件
            db.close(() => {
                fs.rmSync(tempDir, { recursive: true, force: true });
            });
        });

    } catch (err) {
        console.error('处理过程中发生错误:', err);
        if (fs.existsSync(tempDir)) {
            fs.rmSync(tempDir, { recursive: true, force: true });
        }
    }
}

// 获取命令行参数
const args = process.argv.slice(2);
convertApkg(args[0]);
