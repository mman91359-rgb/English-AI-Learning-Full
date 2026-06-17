import pymysql

conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', password='123456', database='exercise_db')
cursor = conn.cursor()

# ===== 题目数据 =====
vocab_questions = [
    ("The company was ___ in 2005 and has grown rapidly since then.",
     '["A. established", "B. establishing", "C. establish", "D. to establish"]',
     "A. established", "被动语态，公司被成立于2005年。was + 过去分词", 3, 1),
    ("She is very ___ about her chances of winning the scholarship.",
     '["A. optimistic", "B. optional", "C. obvious", "D. opposite"]',
     "A. optimistic", "be optimistic about = 对…乐观，固定搭配", 3, 5),
    ("The professor asked us to ___ the experiment and write a report.",
     '["A. conduct", "B. produce", "C. make", "D. create"]',
     "A. conduct", "conduct the experiment = 进行实验", 2, 1),
    ("There is a growing ___ that climate change needs immediate action.",
     '["A. awareness", "B. attention", "C. concern", "D. recognition"]',
     "A. awareness", "growing awareness = 日益增长的意识", 4, 3),
    ("He has a remarkable ___ for learning new languages.",
     '["A. ability", "B. talent", "C. capacity", "D. skill"]',
     "C. capacity", "capacity for = 做某事的能力/容量", 5, 3),
    ("The new policy will come into ___ next month.",
     '["A. effect", "B. affect", "C. effort", "D. efficiency"]',
     "A. effect", "come into effect = 生效。affect是动词, effect是名词", 4, 5),
    ("We need to ___ the problem before making a decision.",
     '["A. analyze", "B. analysis", "C. analyst", "D. analytical"]',
     "A. analyze", "need to + 动词原形。analyze是动词", 2, 1),
    ("The city has made ___ progress in reducing pollution.",
     '["A. significant", "B. significantly", "C. significance", "D. signify"]',
     "A. significant", "形容词修饰名词progress", 3, 4),
    ("It is ___ that regular exercise benefits mental health.",
     '["A. generally", "B. general", "C. generous", "D. genuine"]',
     "A. generally", "It is generally + 过去分词", 3, 4),
    ("The doctor ___ him to take the medicine three times a day.",
     '["A. advised", "B. suggested", "C. insisted", "D. demanded"]',
     "A. advised", "advise sb to do sth。suggest后不接sb to do", 5, 5),
    ("Her performance in the exam was beyond our ___.",
     '["A. expectations", "B. expression", "C. experience", "D. explanation"]',
     "A. expectations", "beyond expectations = 超出预期", 3, 3),
    ("These two words are similar in meaning but ___ in usage.",
     '["A. differ", "B. different", "C. difference", "D. differently"]',
     "A. differ", "differ = 不同(动词)", 3, 1),
    ("The government is taking measures to ___ unemployment.",
     '["A. reduce", "B. produce", "C. introduce", "D. deduce"]',
     "A. reduce", "reduce unemployment = 减少失业", 2, 1),
    ("I can hardly ___ the difference between these two pictures.",
     '["A. distinguish", "B. divide", "C. separate", "D. differ"]',
     "A. distinguish", "distinguish the difference = 分辨差异", 4, 6),
    ("The book provides a detailed ___ of modern Chinese history.",
     '["A. account", "B. amount", "C. count", "D. discount"]',
     "A. account", "a detailed account of = 对…的详细描述", 6, 3),
    ("He is one of the most ___ writers in contemporary literature.",
     '["A. influential", "B. influenced", "C. influencing", "D. influence"]',
     "A. influential", "influential = 有影响力的(形容词)", 4, 4),
    ("Many students find it difficult to ___ to college life.",
     '["A. adapt", "B. adopt", "C. adjust", "D. apply"]',
     "A. adapt", "adapt to = 适应。adopt = 采纳", 5, 6),
    ("The research ___ that students who read more perform better.",
     '["A. indicates", "B. increases", "C. influences", "D. insists"]',
     "A. indicates", "indicate = 表明/显示", 2, 1),
    ("We should take ___ of this opportunity to improve our skills.",
     '["A. advantage", "B. advance", "C. advice", "D. adventure"]',
     "A. advantage", "take advantage of = 利用", 3, 5),
    ("His ___ to the problem impressed all the experts.",
     '["A. approach", "B. arrival", "C. approval", "D. appearance"]',
     "A. approach", "approach to the problem = 解决问题的方法", 4, 3),
    ("The company plans to ___ its business to overseas markets.",
     '["A. expand", "B. extend", "C. expense", "D. express"]',
     "A. expand", "expand the business = 扩展业务", 2, 1),
    ("She is ___ to be one of the best teachers in our school.",
     '["A. considered", "B. regarded", "C. thought", "D. believed"]',
     "A. considered", "be considered to be = 被认为", 5, 5),
    ("A healthy diet is ___ to maintaining good health.",
     '["A. essential", "B. especial", "C. special", "D. specific"]',
     "A. essential", "be essential to = 对…至关重要", 2, 4),
    ("The students showed great ___ in solving the math problem.",
     '["A. enthusiasm", "B. entertainment", "C. environment", "D. equipment"]',
     "A. enthusiasm", "show enthusiasm in = 表现出热情", 3, 3),
    ("We cannot ___ the importance of a good education.",
     '["A. ignore", "B. imagine", "C. identify", "D. illustrate"]',
     "A. ignore", "cannot ignore the importance = 不能忽视重要性", 3, 1),
    ("His success is largely ___ to his hard work and dedication.",
     '["A. due", "B. because", "C. owing", "D. thanks"]',
     "A. due", "be due to = 由于/归功于", 4, 5),
    ("Reading can help to ___ your vocabulary significantly.",
     '["A. enrich", "B. enforce", "C. ensure", "D. enable"]',
     "A. enrich", "enrich vocabulary = 丰富词汇量", 2, 1),
    ("The manager ___ the importance of team cooperation.",
     '["A. emphasized", "B. employed", "C. enabled", "D. encouraged"]',
     "A. emphasized", "emphasize the importance = 强调重要性", 2, 1),
    ("The committee finally ___ a decision after three hours of discussion.",
     '["A. reached", "B. arrived", "C. came", "D. got"]',
     "A. reached", "reach a decision = 做出决定", 6, 6),
    ("The evidence was ___ to prove his innocence beyond doubt.",
     '["A. sufficient", "B. efficient", "C. proficient", "D. deficient"]',
     "A. sufficient", "sufficient = 足够的。efficient = 高效的", 7, 4),
]

grammar_questions = [
    ("If I ___ you, I would accept the offer immediately.",
     '["A. were", "B. am", "C. was", "D. be"]',
     "A. were", "虚拟语气：与现在事实相反，be动词用were", 4, 10),
    ("The book ___ I borrowed from the library is very interesting.",
     '["A. which", "B. who", "C. what", "D. when"]',
     "A. which", "定语从句：先行词the book是物，用which引导", 3, 9),
    ("Not until he arrived ___ the truth of the matter.",
     '["A. did he know", "B. he knew", "C. he did know", "D. knew he"]',
     "A. did he know", "倒装句：Not until在句首，主句部分倒装", 6, 11),
    ("She has been studying English ___ she was ten years old.",
     '["A. since", "B. for", "C. when", "D. while"]',
     "A. since", "现在完成进行时 + since + 过去时间点", 2, 8),
    ("It was in the library ___ I met Professor Wang yesterday.",
     '["A. that", "B. where", "C. which", "D. when"]',
     "A. that", "强调句型: It is/was…that…", 5, 8),
    ("By the time he arrives, we ___ for two hours.",
     '["A. will have been waiting", "B. will wait", "C. waited", "D. have waited"]',
     "A. will have been waiting", "将来完成进行时", 7, 8),
    ("The teacher, as well as the students, ___ excited about the trip.",
     '["A. is", "B. are", "C. were", "D. have been"]',
     "A. is", "主谓一致：as well as连接时与第一个名词一致", 5, 15),
    ("I wish I ___ harder when I was in college.",
     '["A. had studied", "B. studied", "C. would study", "D. study"]',
     "A. had studied", "wish+虚拟语气：与过去事实相反用过去完成时", 5, 10),
    ("___ is known to all, the earth moves around the sun.",
     '["A. As", "B. It", "C. That", "D. What"]',
     "A. As", "as引导非限制性定语从句: 众所周知", 4, 9),
    ("He talked as if he ___ everything about the project.",
     '["A. knew", "B. knows", "C. has known", "D. will know"]',
     "A. knew", "as if+虚拟语气：与现在事实相反用一般过去时", 6, 10),
    ("___ hard he tries, he never seems to satisfy his boss.",
     '["A. However", "B. Whatever", "C. Whoever", "D. Whenever"]',
     "A. However", "However + adj/adv + 主语 + 谓语 = 无论多么", 5, 14),
    ("The house ___ roof was damaged in the storm has been repaired.",
     '["A. whose", "B. which", "C. that", "D. its"]',
     "A. whose", "定语从句：whose表示所属关系", 3, 9),
    ("Only by working together ___ solve this difficult problem.",
     '["A. can we", "B. we can", "C. we will", "D. will we"]',
     "A. can we", "Only+状语在句首，主句部分倒装", 5, 11),
    ("What she said suggested that she ___ unhappy with the result.",
     '["A. was", "B. be", "C. is", "D. were"]',
     "A. was", "suggest此处表暗示而非建议，不用虚拟语气", 6, 10),
    ("The number of students who ___ cars ___ increasing rapidly.",
     '["A. own ... is", "B. owns ... are", "C. own ... are", "D. owns ... is"]',
     "A. own ... is", "who指代students(复数)用own; The number of用单数", 6, 15),
    ("Had I known the result earlier, I ___ a different decision.",
     '["A. would have made", "B. would make", "C. made", "D. have made"]',
     "A. would have made", "虚拟语气省略if的倒装：Had I known = If I had known", 7, 10),
    ("He is the only one of the students who ___ passed the test.",
     '["A. has", "B. have", "C. had", "D. having"]',
     "A. has", "the only one of + 复数名词 + who + 单数谓语", 6, 15),
    ("___ we have finished the course, we shall start revision.",
     '["A. Now that", "B. So that", "C. In that", "D. Except that"]',
     "A. Now that", "Now that = 既然。引导原因状语从句", 4, 14),
    ("There is no point ___ about the past; we should focus on the future.",
     '["A. in complaining", "B. to complain", "C. complaining", "D. complain"]',
     "A. in complaining", "There is no point in doing sth = 做某事没有意义", 5, 4),
    ("It is high time that we ___ actions to protect the environment.",
     '["A. took", "B. take", "C. have taken", "D. will take"]',
     "A. took", "It is high time that + 过去时(虚拟语气)", 6, 10),
]

print("=== 插入题目 ===")
count = 0

for stem, options, answer, analysis, difficulty, kp_id in vocab_questions:
    cursor.execute(f"INSERT INTO question_info (module_type, target_level, difficulty_level, question_type, knowledge_point_id, source) VALUES (1, 'CET4', {difficulty}, 'CHOICE', {kp_id}, 'MANUAL')")
    qid = cursor.lastrowid
    cursor.execute("INSERT INTO question_content (question_id, stem, options, correct_answer, analysis) VALUES (%s, %s, %s, %s, %s)", (qid, stem, options, answer, analysis))
    cursor.execute(f"INSERT INTO knowledge_mapping (question_id, knowledge_point_id) VALUES ({qid}, {kp_id})")
    count += 1

for stem, options, answer, analysis, difficulty, kp_id in grammar_questions:
    cursor.execute(f"INSERT INTO question_info (module_type, target_level, difficulty_level, question_type, knowledge_point_id, source) VALUES (2, 'CET4', {difficulty}, 'CHOICE', {kp_id}, 'MANUAL')")
    qid = cursor.lastrowid
    cursor.execute("INSERT INTO question_content (question_id, stem, options, correct_answer, analysis) VALUES (%s, %s, %s, %s, %s)", (qid, stem, options, answer, analysis))
    cursor.execute(f"INSERT INTO knowledge_mapping (question_id, knowledge_point_id) VALUES ({qid}, {kp_id})")
    count += 1

conn.commit()

# 验证
cursor.execute("SELECT COUNT(*) FROM question_info")
total = cursor.fetchone()[0]
print(f"总计题目: {total} 道")

cursor.execute("SELECT module_type, COUNT(*) FROM question_info GROUP BY module_type")
for row in cursor.fetchall():
    name = "词汇" if row[0] == 1 else "语法"
    print(f"  {name}: {row[1]} 道")

cursor.execute("SELECT COUNT(*) FROM knowledge_mapping")
print(f"知识点映射: {cursor.fetchone()[0]} 条")

cursor.execute("SELECT MIN(difficulty_level), MAX(difficulty_level) FROM question_info")
mn, mx = cursor.fetchone()
print(f"难度范围: {mn} ~ {mx}")

conn.close()
print("\n=== 种子数据插入完成 ===")
