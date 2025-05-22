import react, {useState, useEffect} from 'react';
import '../Style/Sidebar.css';
import ContactList from './ContactList';

function Sidebar({profileImage}){

    const [contacts,setContacts]=useState([]);
    const [loading,setLoading]=useState([true]);
    const [error,setError]=useState([null]);

    useEffect(() => {
        const fetchChatRooms = async () => {
            
        }
    })


    return(        
        <>
            <div className='Sidebar'>
                <p className='chatTitle'>Chats</p>
                <form className='searchUser'>
                    <input className='searchfield' placeholder='Search'/>
                </form>
                <br></br>
                <div className='chatHistory'>
                    <div className='userInfoList'>
                        <img src={profileImage} alt='User Avatar' className='useravatar'></img>
                        Arsh
                    </div>
                </div>
            </div>
        </>   
    );
}

export default Sidebar;
