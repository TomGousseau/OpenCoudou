-- Sides Library
-- Provides constants for sides/directions

local sides = {
  bottom = 0,
  down = 0,
  negy = 0,
  
  top = 1,
  up = 1,
  posy = 1,
  
  back = 2,
  north = 2,
  negz = 2,
  
  front = 3,
  south = 3,
  posz = 3,
  forward = 3,
  
  right = 4,
  west = 4,
  negx = 4,
  
  left = 5,
  east = 5,
  posx = 5
}

-- Reverse lookup
sides[0] = "bottom"
sides[1] = "top"
sides[2] = "back"
sides[3] = "front"
sides[4] = "right"
sides[5] = "left"

return sides
