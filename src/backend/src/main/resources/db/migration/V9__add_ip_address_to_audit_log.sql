-- Add IP address column to audit log table for security tracking
-- Supports both IPv4 (15 chars) and IPv6 (45 chars) addresses

ALTER TABLE historial_cambios 
ADD COLUMN ip_address VARCHAR(45);

-- Add comment for documentation
COMMENT ON COLUMN historial_cambios.ip_address IS 'IP address of the user who made the change (IPv4 or IPv6)';
