import React, { useState, useEffect} from 'react';
import AuthPage from './Components/AuthPage.jsx' ;
import ChatWindow from './Components/ChatWindow.jsx';
import Sidebar from './Components/Sidebar.jsx';


function App() {

  const [profileImage, setProfileImage] = useState("");
  
  useEffect(() => {
      const storedAvatar = sessionStorage.getItem("profileImage");
      if (storedAvatar){
        setProfileImage(storedAvatar);
      }
      else{
        const profile = Math.floor(Math.random() * 15) + 1;
        const avatarPath = `./avatars/${profile}.png`;
        setProfileImage(avatarPath);   
        sessionStorage.setItem("profileImage", avatarPath);
      }
  }, []);

  return(
    <>
      <AuthPage/>
    </>
  );
}

export default App
