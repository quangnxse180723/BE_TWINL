-- Thêm dữ liệu Categories
INSERT INTO categories (name) VALUES ('Nữ') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name) VALUES ('Nam') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name) VALUES ('Trẻ em') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name) VALUES ('Thể thao') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name) VALUES ('Phụ kiện') ON CONFLICT (name) DO NOTHING;
-- Thêm dữ liệu Colors
INSERT INTO colors (name) VALUES ('Trắng') ON CONFLICT (name) DO NOTHING;
INSERT INTO colors (name) VALUES ('Đen') ON CONFLICT (name) DO NOTHING;
INSERT INTO colors (name) VALUES ('Đỏ') ON CONFLICT (name) DO NOTHING;
INSERT INTO colors (name) VALUES ('Xanh lá') ON CONFLICT (name) DO NOTHING;
INSERT INTO colors (name) VALUES ('Xanh dương') ON CONFLICT (name) DO NOTHING;
INSERT INTO colors (name) VALUES ('Vàng') ON CONFLICT (name) DO NOTHING;
INSERT INTO colors (name) VALUES ('Hồng') ON CONFLICT (name) DO NOTHING;
INSERT INTO colors (name) VALUES ('Cam') ON CONFLICT (name) DO NOTHING;
INSERT INTO colors (name) VALUES ('Xám') ON CONFLICT (name) DO NOTHING;

-- Thêm dữ liệu Sizes (Không có bảng sizes riêng, sizes được lưu là String Array trong entity Product, nhưng ở đây chỉ là script tham khảo nếu có bảng sizes, hiện tại BE đã hỗ trợ string sizes)
