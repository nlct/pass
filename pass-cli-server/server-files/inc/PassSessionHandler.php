<?php
/*
 * Server PASS session handler  (C) Nicola L. C. Talbot 2020
 */

class PassSessionHandler implements SessionHandlerInterface
{
   private $pass;

   public function __construct(&$pass)
   {
      $this->pass = $pass;
   }

   public function open($sess_path, $sess_name)
   {
      return true;
   }
   
   public function close()
   {
      return true;
   }
   
   public function read($sess_id)
   {
      $stmt = $this->pass->db_prepare(
        "SELECT session_id, date_touched, data FROM sessions WHERE session_id =?");

      if ($stmt === false)
      {
         return '';
      }

      $stmt->bind_param("s", $sess_id);

      if (!$stmt->execute())
      {
         $this->pass->db_error("failed to execute statement.");
         $stmt->close();
         return '';
      }

      $currentTime = time();

      $session_id = null;
      $session_data_touched = null;
      $session_data = null;

      $stmt->bind_result($session_id, $session_date_touched, $session_data);

      $stmt->fetch();

      $stmt->close();

      if (!isset($session_data))
      {
         $session_data = '';
         $stmt = $this->pass->db_prepare(
           "INSERT IGNORE INTO sessions (date_touched, session_id) VALUES (?,?)");
      }
      else
      {
         $stmt = $this->pass->db_prepare(
          "UPDATE sessions SET date_touched=? WHERE session_id=?");
      }

      if ($stmt !== false)
      {
         $stmt->bind_param("is", $currentTime, $sess_id);

         if (!$stmt->execute())
         {
            $this->pass->db_error("failed to execute statement.");
         }

         $stmt->close();
      }

      return $session_data;
   }
   
   public function write($sess_id, $data)
   {
      $currentTime = time();

      $user_id = null;

      if (isset($_SESSION['user_id']))
      {
         $user_id = $_SESSION['user_id'];
      }

      $stmt = $this->pass->db_prepare(
        "UPDATE sessions SET data=?, date_touched=?, user_id=? WHERE session_id=?");

      if ($stmt === false)
      {
         return false;
      }

      $stmt->bind_param("siis", $data, $currentTime, $user_id, $sess_id);

      $stmt->execute();

      $stmt->close();

      return true;
   }
   
   public function destroy($sess_id)
   {
      $stmt = $this->pass->db_prepare("DELETE FROM sessions WHERE session_id=?");

      if ($stmt === false)
      {
         return false;
      }

      $stmt->bind_param("s", $sess_id);

      $stmt->execute();

      $stmt->close();

      return true;
   }
   
   public function gc($sess_maxlifetime)
   {
      $currentTime = time();

      $this->pass->db_query(
        sprintf("DELETE FROM sessions WHERE date_touched + %d < %d",
          $sess_maxlifetime , $currentTime)
      );

      return true;
   }

}
?>
