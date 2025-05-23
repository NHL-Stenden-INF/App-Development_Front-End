-- Function to get user's friends using the user_attributes approach
CREATE OR REPLACE FUNCTION get_user_friends()
RETURNS SETOF JSON AS $$
DECLARE
    current_user_id UUID;
    friend_record RECORD;
BEGIN
    -- Get the current user's ID from the JWT token
    current_user_id := auth.uid();
    
    -- Get the friends array from the user_attributes table
    FOR friend_record IN 
        SELECT unnest(friends) AS friend_id 
        FROM user_attributes 
        WHERE user_id = current_user_id
    LOOP
        -- Return each friend ID as a JSON object
        RETURN NEXT json_build_object('friend_id', friend_record.friend_id);
    END LOOP;
    
    RETURN;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER; 