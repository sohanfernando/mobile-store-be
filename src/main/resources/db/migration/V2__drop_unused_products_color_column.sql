-- Leftover from before color variants existed. Nothing in the codebase reads or writes
-- Product.color (only ProductColorVariant.color is used) - confirmed via grep before dropping.
ALTER TABLE products DROP COLUMN color;
