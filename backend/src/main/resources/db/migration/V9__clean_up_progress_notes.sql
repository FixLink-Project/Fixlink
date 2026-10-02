-- V9: Làm sạch ghi chú tiến trình công việc, loại bỏ các cụm từ nội bộ
UPDATE work_progress
SET note = REPLACE(note, ' (ai nhận trước được trước)', '')
WHERE note LIKE '%(ai nhận trước được trước)%';

UPDATE quotations
SET solution = REPLACE(solution, ' (ai nhận trước được trước)', '')
WHERE solution LIKE '%(ai nhận trước được trước)%';
