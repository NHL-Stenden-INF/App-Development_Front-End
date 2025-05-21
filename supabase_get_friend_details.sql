-- Create a function to get friend details
-- This function will look up user_attributes data for a friend
-- If no data exists, it will provide default values that ensure the app doesn't crash

-- Function to get detailed friend info
CREATE OR REPLACE FUNCTION public.get_friend_details(friend_id UUID)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    user_data jsonb;
    user_points int;
    user_pic text;
    display_name text;
BEGIN
    -- Try to get user attributes
    SELECT points, profile_picture INTO user_points, user_pic
    FROM public.user_attributes
    WHERE user_id = friend_id;
    
    -- If no user attributes found, use defaults
    IF user_points IS NULL THEN
        -- Check if we can get points from a different table or calculation
        -- For now, use a default value
        user_points := 0;
    END IF;
    
    IF user_pic IS NULL OR user_pic = '' THEN
        -- Use an empty string for no profile picture
        user_pic := '';
    END IF;
    
    -- Try to get display name from auth.users if available
    BEGIN
        SELECT raw_user_meta_data->'display_name' INTO display_name
        FROM auth.users
        WHERE id = friend_id;
    EXCEPTION WHEN OTHERS THEN
        display_name := 'User ' || SUBSTRING(friend_id::text, 1, 8);
    END;
    
    -- Build the response object
    user_data := jsonb_build_object(
        'user_id', friend_id,
        'points', user_points,
        'profile_picture', user_pic,
        'display_name', display_name
    );
    
    -- Return as an array to match the format of regular API responses
    RETURN jsonb_build_array(user_data);
END;
$$;

-- Grant permission for all users to execute this function
GRANT EXECUTE ON FUNCTION public.get_friend_details(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_friend_details(UUID) TO anon;

-- You can test this function with:
-- SELECT get_friend_details('put-a-valid-uuid-here'); 