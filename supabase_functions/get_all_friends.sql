-- Function to get all friends for the current authenticated user with timestamp to defeat caching
CREATE OR REPLACE FUNCTION get_all_friends()
RETURNS SETOF JSON AS $$
DECLARE
    current_user_id UUID;
BEGIN
    -- Get the current user's ID from the JWT token
    current_user_id := auth.uid();
    
    -- Return all friends for the current user with current timestamp to prevent caching
    RETURN QUERY
    SELECT json_build_object(
        'friend_id', friend_id,
        'timestamp', extract(epoch from now())::text
    )
    FROM friendships
    WHERE user_id = current_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER; 