-- Function to add a friend with duplicate checking
CREATE OR REPLACE FUNCTION add_friend(friend_id UUID)
RETURNS VOID AS $$
DECLARE
    current_user_id UUID;
BEGIN
    -- Get the current user's ID from the JWT token
    current_user_id := auth.uid();
    
    -- Make sure we don't add ourselves as a friend
    IF current_user_id = friend_id THEN
        RAISE EXCEPTION 'Cannot add yourself as a friend';
    END IF;
    
    -- Check if this friendship already exists
    IF EXISTS (
        SELECT 1 FROM friendships 
        WHERE (user_id = current_user_id AND friend_id = friend_id)
    ) THEN
        -- Friendship already exists, do nothing
        RETURN;
    END IF;
    
    -- Insert the new friendship
    INSERT INTO friendships (user_id, friend_id)
    VALUES (current_user_id, friend_id);
    
    -- Ensure user attributes exist for the friend
    -- This helps with retrieving friend data later
    PERFORM get_or_create_user_attributes(friend_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get or create user attributes for a user
CREATE OR REPLACE FUNCTION get_or_create_user_attributes(input_user_id UUID)
RETURNS TABLE (
    user_id UUID,
    points INTEGER,
    profile_picture TEXT,
    opened_daily_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    -- Check if user attributes exist
    IF EXISTS (SELECT 1 FROM user_attributes WHERE user_id = input_user_id) THEN
        -- Return existing attributes
        RETURN QUERY
        SELECT 
            ua.user_id,
            ua.points,
            ua.profile_picture,
            ua.opened_daily_at
        FROM user_attributes ua
        WHERE ua.user_id = input_user_id;
    ELSE
        -- Create new attributes with default values
        INSERT INTO user_attributes (user_id, points, profile_picture, opened_daily_at)
        VALUES (input_user_id, 0, '', NOW())
        RETURNING user_id, points, profile_picture, opened_daily_at;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER; 