-- Function to get just the friend IDs for the current user
CREATE OR REPLACE FUNCTION get_user_friend_ids()
RETURNS SETOF UUID AS $$
DECLARE
    current_user_id UUID;
BEGIN
    -- Get the current user's ID from the JWT token
    current_user_id := auth.uid();
    
    -- Return just the friend_id values
    RETURN QUERY
    SELECT friend_id
    FROM friendships
    WHERE user_id = current_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER; 