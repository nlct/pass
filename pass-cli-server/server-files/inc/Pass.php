<?php
/*
   Copyright 2022 Nicola L. C. Talbot

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 * Server PASS class
 * Main class for Server PASS website.
 *
 * New instance:
 *  $pass = new Pass(); // default page title
 *  $pass = new Pass('Page Title');
 *
 *  This automatically starts a new session. If the user is logged in $pass->getUserRole() will return 'student', 'staff' or 'admin'. Otherwise false.
 *
 *  For a page that requires the user to be logged in use:
 *
 *  $pass->require_login();
 *
 *  *before* any content is written to STDOUT. This will redirect
 *  the user to the login page and will exit the script if the user
 *  hasn't logged in. This will also deny access to blocked accounts.
 *
 *  A user with 2FA enabled is partially logged in after they have
 *  supplied their password but not fully authenticated until they
 *  have passed the second step. The require_login function will
 *  redirect to the 2FA page if a user with 2FA enabled hasn't yet passed the
 *  second verification step. 
 *
 *  For a page that shouldn't be accessed if the user *is* logged in:
 *
 *  if ($pass->getUserRole())
 *  {// already logged in
 *     $pass->redirect_header();// redirect to home page
 *  }
 *  else
 *  {// do something
 *  }
 *
 *  For a page that requires a specific role, e.g. not 'student':
 *
 *  $pass->require_login();
 *
 *  if ($pass->getUserRole() === Pass::ROLE_STUDENT)
 *  {
 *     // show forbidden response
 *  }
 *  else
 *  {
 *     // display page
 *  }
 *
 *  Alternatively;
 *
 *  if ($pass->isUserStaffOrAdmin())
 *  {
 *     // staff and admin code
 *  }
 *  else
 *  {
 *     // not staff or admin code
 *  }
 *
 *  To display a page:
 *
 *  $pass->page_header(); // prints header, starts body and prints <h1>page title</h1>
 *  // print page content
 *  $pass->page_footer(); // prints footer, ends body and html
 *
 *  To fetch a script parameter use one of the get_xxx_variable methods. Use
 *  $pass->has_errors() to find out if an error occurred (e.g. invalid value).
 *  The errors can be displayed with $pass->print_errors()
 *
 */

require_once __DIR__ . '/PassSessionHandler.php';
require_once __DIR__ . '/PassConfig.php';
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/general.php';
require_once __DIR__ . '/qrcode.php';

class Pass
{
   private $params;
   private $user=null;
   private $coursedata;
   private $assignmentdata;
   private $passdb;
   private $pageTitle;

   private $supplyDateTimeFallback = null;

   private $passConfig;

   private $config = array(// initialise in case database doesn't connect
      'debug'=>array('value'=>1, 'id'=>1)
   );

   const COOKIE_NAME_TRUST = 'pass_skipotp';

   const STATUS_ACTIVE='active';
   const STATUS_PENDING='pending';
   const STATUS_BLOCKED='blocked';

   const STATUS_OPTIONS = array(
      self::STATUS_ACTIVE, 
      self::STATUS_PENDING,
      self::STATUS_BLOCKED);

   const ROLE_STUDENT='student';
   const ROLE_STAFF='staff';
   const ROLE_ADMIN='admin';

   const ROLE_OPTIONS = array(
      self::ROLE_STUDENT, 
      self::ROLE_STAFF,
      self::ROLE_ADMIN);

   /**
    * Date-time format required by pass-cli-server
    */
   const TIME_STAMP_FORMAT = 'Y-m-d\THisvO';

   const CONFIG_TEXT_ALLOWED_TAGS = '<a><span><em><strong><code><ul><ol><li><dl><dt><dd><p>'; 

   const LAST_PAGE_URL_MAXLENGTH = 256;

   /*
    * Patterns are used client-side in HTML forms as an aide for the user, to help
    * them get the correct format. Regular expressions are for
    * server-side PHP validation.
    */ 

   const SUB_PATH_PATTERN = '(([\w\d\-]+(/[\w\d\-]+)*)|\.)';
   const SUB_PATH_PREG = '%^(([\w\d\-]+(/[\w\d\-]+)*)|\.)$%';

   const TOKEN_PATTERN = '[0-9a-fA-F]+';
   const TOKEN_MAXLENGTH = 64;
   const TOKEN_PREG = '/^[0-9a-fA-F]{64}$/';

   const FROM_PREG = '~(/[a-zA-Z0-9_\-]+)+(/|.php(\?.*)?)?~';

   const MIN_PASSWORD_LENGTH = 8;

   /*
    * Maximum size of the users.password field in the database.
    * The length depends on the algorithm used by password_hash.
    */
   private const MAX_HASHED_PASSWORD_LENGTH = 255;

   /*
    * Most of the time, there's no need to fetch all the fields for
    * a user's details. The main ones are: id (UID), username,
    * regnum (registration number), role (student, staff or admin),
    * status (pending, active or blocked), mfa (has 2FA enabled),
    * 2fa_key_verified (2FA key has been verified). The
    * account_creation (account creation date) is only really needed
    * for admin pages, but may as well get that at the same time.
    */ 
   private const SQL_SELECT_USER_FIELDS = 'id, username, regnum, role, mfa, account_creation, status, `2fa_key_verified`';

   /**
    * Sensitive fields that should only be fetched for verification.
    */ 
   private const SQL_SELECT_AUTH_FIELDS = 'password, `2fa_key`';

   /**
     * PASS language labels. These need to match the list in
     * com.dickimawbooks.passlib.AssignmentData used by the backend.
     */
   const FILE_TYPES = array(
      'PDF',
      'DOC',
      'Plain Text',
      'ABAP',
      "ACMscript",
      "ACM",
      "ACSL",
      "ADA",
      "Algol",
      "Ant",
      "Assembler",
      "Awk",
      "bash",
      "Basic",
      "C",
      "C++",
      "Caml",
      "CIL",
      "Clean",
      "Cobol",
      "Comal 80",
      "command.com",
      "Comsol",
      "csh",
      "Delphi",
      "Eiffel",
      "Elan",
      "erlang",
      "Euphoria",
      "Fortran",
      "GAP",
      "GCL",
      "Gnuplot",
      "hansl",
      "Haskell",
      "HTML",
      "IDL",
      "inform",
      "Java",
      "JVMIS",
      "ksh",
      "Lingo",
      "Lisp",
      "LLVM",
      "Logo",
      "Lua",
      "make",
      "Mathematica",
      "Matlab",
      "Mercury",
      "MetaPost",
      "Miranda",
      "Mizar",
      "ML",
      "Modula-2",
      "MuPAD",
      "NASTRAN",
      "Oberon-2",
      "OCL",
      "Octave",
      "Oz",
      "Pascal",
      "Perl",
      "PHP",
      "PL/I",
      "Plasm",
      "PostScript",
      "POV",
      "Prolog",
      "Promela",
      "PSTricks",
      "Python",
      "R",
      "Reduce",
      "Rexx",
      "RSL",
      "Ruby",
      "S",
      "SAS",
      "Scala",
      "Scilab",
      "sh",
      "SHELXL",
      "Simula",
      "SPARQL",
      "SQL",
      "tcl",
      "TeX",
      "VBScript",
      "Verilog",
      "VHDL",
      "VRML",
      "XML",
      "XSLT"
   );

   /**
    * Used by web scripts (which may know the title now or may need
    * to update the title after parsing the parameters) and also by
    * the backend passconsumer.php (which doesn't need a title and
    * doesn't have session data but does need access to the database).
    * @param $title the web page title (may be null)
    */ 
   public function __construct($title=null)
   {
      $this->passConfig = new PassConfig($this);

      $this->params = array();

      if (isset($title))
      {
         $this->pageTitle = $this->getShortTitle() . ": $title";
      }
      else
      {
         $this->pageTitle = $this->getLongTitle();
      }

      $this->init_session();
   }

   function __destruct()
   {
      $this->db_disconnect();
   }

   /**
     * Initialises session, connects to database, and stores user details
     * in Pass::user if logged in.
     */
   public function init_session()
   {
      if (!$this->db_connect()) return false;

      if (session_set_save_handler(new PassSessionHandler($this)) === false)
      {
         $this->log_error("Unable set session save handler");
	 return false;
      }

      if (!session_start()) return false;

      if (isset($_SESSION['coursedata']))
      {
         $this->coursedata = $_SESSION['coursedata'];
      }

      if (isset($_SESSION['assignmentdata']))
      {
         $this->assignmentdata = $_SESSION['assignmentdata'];
      }

      if (isset($_SESSION['user_id']))
      {
         $result = $this->db_query(sprintf("SELECT %s, 2fa_key FROM users WHERE id=%d",
          self::SQL_SELECT_USER_FIELDS, $_SESSION['user_id']));

	 if ($result && mysqli_num_rows($result) == 1)
	 {
            $row = mysqli_fetch_assoc($result);

            $this->user = array('id'=>(int)$row['id'],
              'username'=>$row['username'],
              'role'=>$row['role'],
              'mfa'=>($row['mfa'] === 'on' ? true : false),
              '2fa_key_verified'=>($row['2fa_key_verified'] === 'true' ? true : false),
              'account_creation'=>$row['account_creation'],
              'status'=>$row['status']
            );

            if (!empty($row['regnum']))
            {
               $this->user['regnum'] = $row['regnum'];
            }

            if (!empty($row['2fa_key']))
            {
               $this->user['2fa_key'] = $row['2fa_key'];
            }

            if (isset($_SESSION['requires_verification'])
                  && $_SESSION['requires_verification'] === true)
            {
               $this->user['requires_verification'] = true;
            }
            else
            {
               $this->user['requires_verification'] = false;
            }
	 }
	 else
	 {
            unset($_SESSION['user_id']);
            return false;
	 }
      }

      $this->update_whos_online();

      return true;
   }

   /**
    * Updates the "who's online" information. This enables admin to
    * check if someone is in the middle of an upload when the site
    * needs to go into maintenance mode.
    */ 
   private function update_whos_online()
   {
      if (isset($this->user) && is_numeric($this->user['id']))
      {
         $url = $_SERVER['REQUEST_URI'];

	 if (strlen($url) > self::LAST_PAGE_URL_MAXLENGTH)
	 {
            $url = substr($url, 0, self::LAST_PAGE_URL_MAXLENGTH);
	 }

         $this->db_query(sprintf("INSERT INTO whos_online (user_id,time_last_clicked,last_page_url) VALUES (%d,CURRENT_TIMESTAMP,'%s') AS new ON DUPLICATE KEY UPDATE time_last_clicked=new.time_last_clicked, last_page_url=new.last_page_url",
         $this->user['id'], $this->db_escape_sql($url)));
      }

      $result = $this->db_query("DELETE FROM whos_online WHERE time_last_clicked < DATE_SUB(NOW(), INTERVAL 1 HOUR)");
   }

   /**
    * Records an action to help admin discover the reason when
    * things go wrong. (Otherwise, you'll need to ssh into the site
    * to read the PHP error log file, which isn't convenient.)
    * @param $action the action being performed, which should be a
    * simple alphanumeric label
    * @param $comments information about the action (success or
    * what failed)
    * @param $convert_entities true if the comments need to have
    * entities escape or false if they have already been escaped or
    * contains valid allowed markup
    */ 
   public function record_action($action, $comments=null, $convert_entities=false)
   {
      $action = htmlentities($action);// shouldn't contain any awkward characters, but just in case

      if (isset($comments) && $convert_entities)
      {
         $comments = htmlentities($comments);
      }

      $stmt = $this->db_prepare("INSERT INTO action_recorder SET user_id=?, action=?, comments=?");

      if ($stmt)
      {
         $user_id = $this->getUserID();

         if ($user_id === false) $user_id = null;

         $stmt->bind_param("iss", $user_id, $action, $comments);

         if (!$stmt->execute())
         {
            $this->db_error("Failed to execute statement");
         }

         $stmt->close();
      }
   }

   /**
    * Gets information about the user or users. This provides the
    * following information in a hashed array for a single user: id (UID), 
    * role (student, staff or admin), username (should match
    * supplied username), status (pending, active or blocked),
    * account_creation (when the account was created), and
    * regnum (the registration number, which may be null if not set).
    * The upload form will need to supply a dummy registration
    * number for staff/admin to allow them to test the upload page,
    * but this shouldn't be supplied for the account details page.
    *
    * If the username matches the currently logged in user, there's
    * no need to fetch the information from the database as it's
    * already available in Pass::user (but the dummy registration
    * number will be supplied, if applicable).
    *
    * If an array of usernames is supplied, a hash is returned with
    * the username as the key and the value is a hash as for a
    * single user lookup. If there's no match for all listed
    * usernames, an empty array is returned. If there is no match or
    * only a subset match, an error is logged with Pass:add_error_message
    * and only a hash of found users is returned, so check with Pass::has_errors()
    * afterwards.
    *
    * @param $username single username if string or array of
    * usernames if information for multiple users required
    * @param $supply_non_student_missing_regnum if true, supply the
    * dummy registration number for non-student accounts
    * @return a hash array for single student or false if no match found,
    * or a hash of hashed arrays for multiple students with the
    * username as the key or an empty array if none found
    */ 
   public function getUserInfo($usernames, $supply_non_student_missing_regnum=false)
   {
      if (is_string($usernames))
      {
         if (isset($this->user) && ($usernames === $this->user['username']))
         {
            $data = array('id'=>$this->user['id'], 'role'=>$this->user['role'],
	         'username'=>$this->user['username'],
	         'status'=>$this->user['status'],
	         'account_creation'=>$this->user['account_creation']);

	    if (isset($this->user['regnum']))
	    {
               $data['regnum'] = $this->user['regnum'];
	    }
	    elseif ($this->user['role']!==self::ROLE_STUDENT
                      && $supply_non_student_missing_regnum)
	    {
               $data['regnum'] = PassConfig::DUMMY_REG_NUMBER;
	    }
         }
         else
         {
            $sql_username = $this->db_escape_sql($usernames);

            $sql = sprintf("SELECT %s FROM users WHERE username='%s'",
             self::SQL_SELECT_USER_FIELDS, $sql_username);

	    $result = $this->db_query($sql);
            $data = false;

	    if ($result)
	    {
               $row = mysqli_fetch_assoc($result);

               if ($row)
               {
                  $data = array('id'=>$row['id'],
                    'role'=>$row['role'],
                    'status'=>$row['status'],
                    'account_creation'=>$row['account_creation'],
                    'username'=>$row['username']
                  );

                  if (empty($row['regnum']) && $row['role'] !== self::ROLE_STUDENT
                       && $supply_non_student_missing_regnum)
	          {
                     $data['regnum'] = PassConfig::DUMMY_REG_NUMBER;
	          }
	          elseif (isset($row['regnum']))
	          {
                     $data['regnum'] = $row['regnum'];
	          }
               }
            }
         }

         return $data;
      }
      elseif (is_array($usernames))
      {
         $sql = sprintf("SELECT %s FROM users WHERE username IN (",
           self::SQL_SELECT_USER_FIELDS);

	 $sep = '';

	 foreach ($usernames as $name)
	 {
	    $sql .= "$sep'" . $this->db_escape_sql($name) . "'";

	    $sep = ', ';
	 }

         $sql .= ')';

	 $result = $this->db_query($sql);

	 if ($result)
	 {
            $data = array();

	    while ($row = mysqli_fetch_assoc($result))
            {
	       $record = array(
		       'id'=>(int)$row['id'], 
		       'username'=>$row['username'], 
		       'account_creation'=>$row['account_creation'],
		       'role'=>$row['role'],
                       'status'=>$row['status']);

               if (empty($row['regnum']) && $row['role'] !== self::ROLE_STUDENT
                    && $supply_non_student_missing_regnum)
	       {
                  $record['regnum'] = PassConfig::DUMMY_REG_NUMBER;
	       }
	       elseif (isset($row['regnum']))
	       {
                  $record['regnum'] = $row['regnum'];
	       }

               $data[$row['username']] = $record;
	    }

	    if (count($data) !== count($usernames))
	    {
               foreach ($usernames as $name)
	       {
                  if (!isset($data[$name]))
		  {
                      $this->add_error_message('Failed to find info for user \''
                          . htmlentities($name) . '\'');
		  }
	       }
	    }

	    return $data;
	 }
	 else
	 {
            $this->add_error_message('Failed to read user info.');
	 }
      }
      else
      {
         $this->log_error("invalid argument ".var_export($usernames, true));
      }

      return false;
   }

   /**
    * Gets information about the user or users. Similar to Pass::getUserInfo
    * but references the users by their numeric UID.
    *
    * This method doesn't check if all listed users were found, so
    * check if the length of the returned array matches the length
    * of the supplied array.
    *
    * @param $user_ids a UID if numeric or an array of numeric UID
    * @param $supplyDummyRegNum if true, supply the
    * dummy registration number for non-student accounts
    * @return a hash array for single student or false if no match found,
    * or a hash of hashed arrays for multiple students with the
    * UID as the key or an empty array if none found
    */
   public function getUserInfoByID($user_ids, $supplyDummyRegNum=false)
   {
      if (is_numeric($user_ids))
      {
         if ($user_ids === $this->user['id'])
	 {
            return $this->user;
	 }
	 else
	 {
	    $sql = sprintf("SELECT %s FROM users WHERE id=%d",
               self::SQL_SELECT_USER_FIELDS, $user_ids);

	    $result = $this->db_query($sql);

	    if ($result)
	    {
               $data = array();

	       if ($row = mysqli_fetch_assoc($result))
               {
	          $data = array(
		          'id'=>(int)$row['id'], 
		          'username'=>$row['username'], 
		          'account_creation'=>$row['account_creation'],
		          'role'=>$row['role'],
                          'status'=>$row['status']);

                  if ($supplyDummyRegNum && empty($row['regnum'])
                         && $row['role'] !== self:ROLE_STUDENT)
	          {
                     $data['regnum'] = PassConfig::DUMMY_REG_NUMBER;
	          }
	          else
	          {
                     $data['regnum'] = $row['regnum'];
	          }

                  return $data;
	       }
	       else
	       {
	          return null;
	       }
	    }

	    return false;
	 }
      }
      elseif (is_array($user_ids))
      {
         $sql = sprintf("SELECT %s FROM users WHERE id IN (",
            self::SQL_SELECT_USER_FIELDS);

	 $sep = '';

	 foreach ($user_ids AS $id)
	 {
            if (!is_numeric($id))
	    {
               $this->log_error("User ID '$id' is not numeric");
	       return false;
	    }

	    $sql .= "$sep$id";

	    $sep = ', ';
	 }

         $sql .= ')';

	 $result = $this->db_query($sql);

	 if ($result)
	 {
            $data = array();

	    while ($row = mysqli_fetch_assoc($result))
            {
	       $record = array(
		       'id'=>(int)$row['id'], 
		       'username'=>$row['username'], 
		       'account_creation'=>$row['account_creation'],
		       'role'=>$row['role'],
                       'status'=>$row['status']);

               if ($supplyDummyRegNum && empty($row['regnum'])
                    && $row['regnum'] !== self::ROLE_STUDENT)
	       {
                  $record['regnum'] = PassConfig::DUMMY_REG_NUMBER;
	       }
	       else
	       {
                  $record['regnum'] = $row['regnum'];
	       }

               $data[$row['id']] = $record;
	    }

	    return $data;
	 }
      }
      elseif ($user_ids === true)
      {
         $sql = sprintf("SELECT %s FROM users",
            self::SQL_SELECT_USER_FIELDS);

	 $result = $this->db_query($sql);

	 if ($result)
	 {
            $data = array();

	    while ($row = mysqli_fetch_assoc($result))
            {
	       $record = array(
		       'id'=>(int)$row['id'], 
		       'account_creation'=>$row['account_creation'], 
		       'username'=>$row['username'], 
		       'role'=>$row['role'],
                       'status'=>$row['status']);

               if ($supplyDummyRegNum && empty($row['regnum'])
                     && $row['role'] !== self::ROLE_STUDENT)
	       {
                  $record['regnum'] = PassConfig::DUMMY_REG_NUMBER;
	       }
	       else
	       {
                  $record['regnum'] = $row['regnum'];
	       }

               $data[$row['id']] = $record;
	    }

	    return $data;
	 }
      }
      else
      {
         $this->log_error("invalid argument ".var_export($user_ids, true));
      }

      return false;
   }

   /**
    * Gets user information according to search criteria.
    *
    * The filter may have the following keys:
    * <ul>
    * <li> usernameregex username matches regular expression
    * supplied in value
    * <li> usernamenotregex username does not match regular expression
    * supplied in value
    * <li> regnumregex registration number matches regular expression
    * supplied in value
    * <li> regnumnotregex registration number does not match regular expression
    * supplied in value
    * <li>user_id numeric UID matches supplied numeric value
    * </ul>
    *
    * The each element of the returned array is a hashed array with
    * the keys id (UID), username, role (student, staff, admin),
    * status (pending, active, blocked), account_creation. 
    *
    * @param $filter a hashed array of search criteria
    * @param $boolop should either be 'AND' or 'OR'
    * @param $usehash if true return hashed array of users
    * with the username as the key, otherwise return array of users
    * @return an array of users matching the criteria or an empy
    * array if no match or false if an error occurred
    */ 
   public function getUserInfoByFilter($filter, $boolop='AND', $usehash=true)
   {
      $sql = sprintf("SELECT %s FROM users ",
         self::SQL_SELECT_USER_FIELDS);

      $condition = '';

      if (!empty($filter['usernameregex']))
      {
         $condition = sprintf("WHERE username REGEXP '%s'",
            $this->db_escape_sql($filter['usernameregex']));
      }

      if (!empty($filter['usernamenotregex']))
      {
         if (empty($condition))
	 {
            $condition = 'WHERE ';
	 }
	 else
	 {
            $condition .= " $boolop ";
	 }

         $condition .= sprintf("username NOT REGEXP '%s'",
            $this->db_escape_sql($filter['usernamenotregex']));
      }

      if (!empty($filter['regnumregex']))
      {
         if (empty($condition))
	 {
            $condition = 'WHERE ';
	 }
	 else
	 {
            $condition .= " $boolop ";
	 }

         $condition .= sprintf("IFNULL(regnum,'') REGEXP '%s'",
               $this->db_escape_sql($filter['regnumregex']));
      }

      if (!empty($filter['regnumnotregex']))
      {
         if (empty($condition))
	 {
            $condition = 'WHERE ';
	 }
	 else
	 {
            $condition .= " $boolop ";
	 }

         $condition .= sprintf("IFNULL(regnum,'') NOT REGEXP '%s'",
            $this->db_escape_sql($filter['regnumnotregex']));
      }

      if (isset($filter['user_id']) && is_numeric($filter['user_id']))
      {
         if (empty($condition))
	 {
            $condition = 'WHERE ';
	 }
	 else
	 {
            $condition .= " $boolop ";
	 }

	 $condition .= sprintf("id=%d", $filter['user_id']);
      }

      $result = $this->db_query($sql . ' ' . $condition);

      if ($result)
      {
         $data = array();

	 while ($row = mysqli_fetch_assoc($result))
	 {
	    $record = array('id'=>(int)$row['id'], 'username'=>$row['username'],
               'role'=>$row['role'], 'status'=>$row['status'],
               'account_creation'=>$row['account_creation']
	    );

	    if (isset($row['regnum']))
	    {
               $record['regnum'] = $row['regnum'];
	    }

	    if ($usehash)
	    {
               $data[$row['username']] = $record;
	    }
	    else
	    {
               array_push($data, $record);
	    }
	 }

	 return $data;
      }

      return $false;
   }

   /**
    * Gets the result from querying the members of the project group
    * for the given submission (job) ID.
    * @param $submission_id numeric value identifying the
    * submission/job
    * @return the result from the SELECT query (mysqli_result) or
    * false if query failed or $submission_id not numeric
    */ 
   public function getProjectGroup($submission_id)
   {
      if (!is_numeric($submission_id))
      {
         error_log("Submission ID '$submission_id' is not numeric");
	 return false;
      }

      return $this->db_query(
	      "SELECT * FROM projectgroup WHERE submission_id=$submission_id");
   }

   /**
    * Gets a list of queued submissions order by submission/job ID.
    * This is a list of submissions (uploads) where the status is
    * 'queued', which means the job is still in the queue waiting to
    * be processed. The return value is a hashed array with the
    * submission ID as the key. Each element is also a hashed array
    * with keys 'status' (which will be 'queued') and 'index', (the
    * queue index).
    * @return an array of submissions or an empty array if no queue
    * submissions or false if query failed
    */ 
   public function getQueuedSubmissions()
   {
      $result = $this->db_query("SELECT id, status FROM submissions WHERE status='queued' ORDER BY id");

      if (!$result)
      {
         $this->add_error_message("Database query failed.");
	 return false;
      }

      $data = array();
      $idx = 0;

      while ($row = mysqli_fetch_assoc($result))
      {
         $submission_id = (int)$row['id'];
	 $data[$submission_id] = array('status'=>$row['status'], 'index'=>(++$idx));
      }

      return $data;
   }

   /**
    * Gets the list of final uploads according to the search
    * criteria. A student's final upload is the last upload they
    * made for a given assignment.
    *
    * The $filters parameter, if not null, should be a hashed array
    * with any of the following keys:
    * <ul>
    * <li> course: the course label
    * <li> assignment: the assignment label
    * <li> usernames: an array of usernames (match any supplied usernames)
    * or a string username
    * </ul>
    *
    * The returned array will be a list of matches where each
    * element is a hashed array with the keys 'user_id' (uploader's UID),
    * 'course' (course label), 'assignment' (assignment label),
    * 'role' (uploader's role: student or staff or admin),
    * 'username' (uploader's username), 'timestamp' (the timestamp of the latest
    * submission by the uploader for the assignment),
    * 'num_submissions' (the number of times the uploader uploaded a
    * submission for the assignment)
    *
    * @param $filters array of search criteria or null for a list of all final
    * uploads
    * @return an array of results or an empty array of no match or
    * false if the query failed
    */ 
   public function getFinalUploads($filters=null)
   {
      $sql = "SELECT projectgroup.user_id, submissions.course, submissions.assignment, role, username, MAX(submissions.upload_time) AS timestamp, COUNT(submissions.id) AS num_submissions FROM projectgroup, submissions, users ";

      $where = "WHERE projectgroup.submission_id = submissions.id AND projectgroup.user_id = users.id ";

      if (isset($filters))
      {
         if (isset($filters['course']))
	 {
            $where .= sprintf(" AND submissions.course = '%s'", $this->db_escape_sql($filters['course']));
	 }

         if (isset($filters['assignment']))
	 {
            $where .= sprintf(" AND submissions.assignment = '%s'", $this->db_escape_sql($filters['assignment']));
	 }

	 if (!empty($filters['usernames']))
	 {
	    if (is_array($filters['usernames']))
	    {
               $where. = ' AND (';

               $sep = '';

	       foreach ($filters['usernames'] as $username)
	       {
                  $where .= sprintf("%susername='%s'", 
		       $sep, $this->db_escape_sql(trim($username)));

                  $sep = ' OR ';
	       }

               $where .= ')';
	    }
	    else
	    {
               $where .= sprintf(" AND username='%s'", 
		       $this->db_escape_sql(trim($filters['usernames'])));
	    }
	 }
      }

      $result = $this->db_query("$sql $where GROUP BY submissions.course, submissions.assignment, projectgroup.user_id ORDER BY timestamp DESC");

      if (!$result)
      {
         return false;
      }

      $data = array();

      while ($row = mysqli_fetch_assoc($result))
      {
         array_push($data, $row);
      }

      return $data;
   }

   /**
    * Gets a list of uploads according to the search criteria
    *
    * The $filters parameter, if not null, should be a hashed array
    * with any of the following keys:
    * <ul>
    * <li> course: the course label
    * <li> assignment: the assignment label
    * <li> usernames: a comma-separated list of usernames
    * <li> user_id: a numeric user ID or an array of numeric user IDs or true
    * for all or empty for the logged in user
    * <li> submission_ids: a comma-separated list of numeric
    * submission/job IDs
    * <li> exit_code : match the numeric exit code
    * <li> not_exit_code : not the numeric exit code
    * <li> beforedate : upload time before (or equal to) this time
    * <li> afterdate : upload time after (or equal to) this time
    * </ul>
    *
    * The return list is reverse ordered by the submission ID (that
    * is, newest first). If $usehash parameter is true, the returned
    * array is a hashed array with the submission ID as the key.
    * In either case, each element of the returned array has the
    * following keys:
    * <ul>
    * <li>'submission_id' : the numeric (int) submission/job ID
    * <li>'uploaded_by' : the numeric (int) user ID of the user who
    * uploaded the submission
    * <li>'upload_time' : the upload timestamp (used in the submission directory name)
    * <li>'token' : the submission token (used in the submission directory name) 
    * <li>'course' : the course label
    * <li>'assignment' : the assignment label
    * <li>'status' : the submission status (uploaded, queued,
    * processing, processed)
    * <li>'exit_code' : the numeric exit code or null if not
    * processed
    * <li>'users' : a hashed array of each user listed in the project
    * group with the UID as the key and the value a hashed array
    * with keys: 'username', 'regnum', 'roles'
    * </ul>
    *
    * @param $filters array of search criteria or null for a list of all 
    * uploads
    * @param $usehash true if the return value should be a hashed
    * array
    * @return an array of results or an empty array of no match or
    * false if the query failed
    */ 
   public function getUploads($filters=null, $usehash=true)
   {
      $sql = sprintf("SELECT projectgroup.submission_id, GROUP_CONCAT(projectgroup.user_id SEPARATOR ',') AS group_user_ids, submissions.uploaded_by, submissions.upload_time, submissions.token, submissions.exit_code, submissions.status, submissions.course, submissions.assignment, GROUP_CONCAT(username SEPARATOR ',') AS group_user_names, GROUP_CONCAT(IFNULL(regnum, '%s') SEPARATOR ',') AS group_user_regnums, GROUP_CONCAT(users.role SEPARATOR ',') AS group_user_roles FROM submissions, projectgroup, users WHERE projectgroup.submission_id = submissions.id and projectgroup.user_id=users.id GROUP BY projectgroup.submission_id", $this->db_escape_sql(PassConfig::DUMMY_REG_NUMBER));

      if (isset($filters))
      {
         if (!empty($filters['user_id']))
	 {
            $user_ids = $filters['user_id'];
	 }
	 elseif (!empty($filters['username']))
	 {
            $username = $filters['username'];
	 }

	 if (!empty($filters['submission_ids']))
	 {
            $submission_ids = $filters['submission_ids'];
	 }

	 if (!empty($filters['course']))
	 {
            $course = $filters['course'];
	 }

	 if (!empty($filters['assignment']))
	 {
            $assignment = $filters['assignment'];
	 }
      }

      $having = '';

      if (isset($username))
      {
         $having = sprintf(" HAVING FIND_IN_SET('%s',group_user_names) > 0", 
                  $this->db_escape_sql($username));
      }
      elseif (empty($user_ids))
      {
         $having = sprintf(" HAVING FIND_IN_SET(%d,group_user_ids) > 0", $this->user['id']);
      }
      elseif ($user_ids === true)
      {// any user
      }
      elseif (is_numeric($user_ids))
      {
         $having = sprintf(" HAVING FIND_IN_SET(%d,group_user_ids) > 0", $user_ids);
      }
      elseif (is_array($user_ids))
      {
	 $sep = ' HAVING';

	 foreach ($user_ids as $user_id)
	 {
	    if (is_numeric($user_id))
	    {
               $having .= sprintf("$sep FIND_IN_SET(%d,group_user_ids) > 0", $user_id);
	    }
	    else
	    {
               error_log("User ID '$user_id' is not an integer");
	       return false;
	    }

	    $sep = ' AND';
	 }

      }
      else
      {
         error_log("Invalid user ID " . var_export($user_id, true));
         return false;
      }

      if (isset($course))
      {
         if (empty($having))
	 {
            $having .= ' HAVING ';
	 }
	 else
	 {
            $having .= ' AND ';
	 }

         $having .= sprintf("submissions.course='%s'", $this->db_escape_sql($course));
      }

      if (isset($assignment))
      {
         if (empty($having))
	 {
            $having .= ' HAVING ';
	 }
	 else
	 {
            $having .= ' AND ';
	 }

         $having .= sprintf("submissions.assignment='%s'", $this->db_escape_sql($assignment));
      }

      if (isset($submission_ids))
      {
         if (is_array($submission_ids))
	 {
            if (empty($having))
	    {
               $having .= ' HAVING ';
	    }
	    else
	    {
               $having .= ' AND ';
	    }

            $having .= "projectgroup.submission_id IN (";

	    $sep = '';

	    foreach ($submission_ids as $submission_id)
	    {
               if (is_numeric($submission_id))
	       {
	          $having .= "$sep'$submission_id'";
	          $sep = ', ';
	       }
	       else
	       {
                  error_log("Submission ID '$submission_id' is not an integer");
		  return false;
	       }
	    }

	    $having .= ")";
	 }
	 elseif (is_numeric($submission_ids))
	 {
            if (empty($having))
	    {
               $having .= ' HAVING ';
	    }
	    else
	    {
               $having .= ' AND ';
	    }

            $having .= "projectgroup.submission_id=$submission_ids";
	 }
      }

      if (isset($filters))
      {
         if (isset($filters['exit_code']) && is_numeric($filters['exit_code']))
	 {
            if (empty($having))
	    {
               $having .= ' HAVING ';
	    }
	    else
	    {
               $having .= ' AND ';
	    }

	    $having .= sprintf("exit_code=%d", $filters['exit_code']);
	 }
	 elseif (isset($filters['not_exit_code']) && is_numeric($filters['not_exit_code']))
	 {
            if (empty($having))
	    {
               $having .= ' HAVING ';
	    }
	    else
	    {
               $having .= ' AND ';
	    }

	    $having .= sprintf("exit_code<>%d", $filters['not_exit_code']);
	 }

	 if (isset($filters['beforedate']))
	 {
            if (empty($having))
	    {
               $having .= ' HAVING ';
	    }
	    else
	    {
               $having .= ' AND ';
	    }

	    $having .= sprintf("STRCMP(upload_time, '%s') <= 0", 
		    $this->db_escape_sql($filters['beforedate']));
	 }
	 else if (isset($filters['afterdate']))
	 {
            if (empty($having))
	    {
               $having .= ' HAVING ';
	    }
	    else
	    {
               $having .= ' AND ';
	    }

	    $having .= sprintf("STRCMP(upload_time, '%s') >= 0", 
		    $this->db_escape_sql($filters['afterdate']));
	 }
      }

      $sql .= "$having ORDER BY submissions.id DESC";

      $result = $this->db_query($sql);

      if (!$result)
      {
         return false;
      }

      $data = array();

      while ($row = mysqli_fetch_assoc($result))
      {
         $row_key = (int)$row['submission_id'];

         $entry = array(
            'submission_id' => $row_key,
            'uploaded_by' => (int)$row['uploaded_by'],
            'upload_time' => $row['upload_time'],
            'token' => $row['token'],
            'course' => $row['course'],
            'assignment' => $row['assignment'],
            'status' => $row['status'],
	    'exit_code' => $row['exit_code']
	    );

	 $entry['users'] = array();

	 $user_ids = explode(',', $row['group_user_ids']);
	 $user_names = explode(',', $row['group_user_names']);
	 $user_regnums = explode(',', $row['group_user_regnums']);
	 $user_roles = explode(',', $row['group_user_roles']);

	 for ($i = 0; $i < count($user_ids); $i++)
	 {
            $entry['users'][(int)$user_ids[$i]] = array(
		    'username'=>$user_names[$i],
		    'regnum'=>$user_regnums[$i],
		    'roles'=>$user_roles[$i]
	    );
	 }

	 if ($usehash)
	 {
	    $data[$row_key] = $entry;
	 }
	 else
	 {
            array_push($data, $entry);
	 }
      }

      return $data;
   }

   /**
    * Updates the submission status.
    * @param $submission_id the numeric submission/job ID
    * @param $newstatus the new status ('uploaded','queued','processing','processed')
    * @param $exitCode the numeric exit code if the new status is 'processed' or null if no change
    * @return true if update successful or false if an error
    * occurred
    */ 
   public function updateSubmissionStatus($submission_id, $newstatus, $exitCode=null)
   {
      if (!is_numeric($submission_id))
      {
          error_log('(updateSubmissionStatus) submission ID must be numeric');
	  return false;
      }

      if ($submission_id < 1)
      {
          error_log("(updateSubmissionStatus) invalid submission ID '$submission_id'");
	  return false;
      }

      $sql = "UPDATE submissions SET status='" 
	      . $this->db_escape_sql($newstatus) . "'";

      if (isset($exitCode))
      {
         if (!is_numeric($exitCode))
	 {
            error_log('(updateSubmissionStatus) exit code must be numeric');
	    return false;
	 }

	 $sql .= ", exit_code=$exitCode";
      }
	      
      $sql .= " WHERE id=$submission_id";

      return $this->db_query($sql);
   }

   /**
     * Creates a new account token for password reset or account verification.
     *
     * The purpose of the token is to verify the user's identity
     * (or, rather, it verifies that the user has access to the
     * associated email account). Since the same token table is used for
     * both password resets and account verification, the password
     * reset token could theoretically also be used to verify an account.
     * However, the password can't be reset until the account has been verified.
     * Therefore, tokens for unverified accounts are verification
     * tokens and tokens for verified accounts are password reset
     * tokens.
     *
     * The token is 32 bytes long consisting of a selector and
     * verifier both 16 bytes long.
     *
     * NB not related to the submission token (although this method uses the
     * same function to create the selector and verifier).
     * Also not the trust cookie token for skipping 2FA or the
     * recovery code, which are stored in other tables.
     *
     * @param $user_id the numeric (int) user ID
     * @param $timeout the number of minutes until the token expires
     * (default 30 minutes)
     * @return the combined selector+verifier token to be sent to
     * the user or false if an error occurred
     */

   public function create_account_token(int $user_id, int $timeout=30)
   {
      $selector = create_token(16);
      $verifier = create_token(16);

      $now = new DateTime();
      $expires = $now->add(new DateInterval("PT${timeout}M"));

      $hashed = getHashedVerifier($verifier, $user_id, $expires);

      $sql = sprintf("INSERT INTO tokens SET user_id=%d, selector=X'%s', verifier=X'%s', expires='%s'",
         $user_id,
         $this->db_escape_sql($selector),
         $this->db_escape_sql($hashed),
         $this->db_escape_sql($expires->format('Y-m-d H:i:s')));

      if (!$this->db_query($sql))
      {
         $this->add_generic_error();
         $this->record_action('create_account_token',
            "Insert token statement failed for UID: $user_id");
         return false;
      }

      $this->record_action('create_account_token', 
        "Successfully created token for UID: $user_id");

      return $selector . $verifier;
   }

   /**
    * Verifies an account token. The token should have previously
    * been created with Pass::create_account_token().
    *
    * Before calling this function, check that the token has a valid format. 
    * For example, by obtaining the parameter with Pass::get_token_variable().
    *
    * If successful, the returned array has the keys 'user_id'
    * (int user ID), 'token_id' (int token ID), 'username' the
    * username.
    *
    * @param $token string containing the 64-digit hexadecimal token that needs verifying
    * @return a hashed array containing the user information associated
    * with the token or false if verification failed
    */
   public function verify_account_token($token)
   {
      $selector = substr($token, 0, 32);
      $verifier = substr($token, 32);

      // selector should be hex string but escape anyway

      $sql = sprintf("SELECT tokens.id AS token_id, user_id, verifier, expires, users.username, users.status FROM tokens, users WHERE tokens.user_id=users.id AND selector=X'%s' AND CURRENT_TIMESTAMP < expires",
       $this->db_escape_sql($selector));

      $result = $this->db_query($sql);

      if (!$result)
      {
         $this->add_generic_error();
         return false;
      }

      $user_id = null;
      $token_id = null;
      $username = null;
      $status = null;

      if ($result->num_rows > 0)
      {
         /*
            There should typically only be at most one match but 
            allow for tiny probability that multiple verification
            link requests have been made and there are duplicate selectors.
         */

         while ($row = mysqli_fetch_assoc($result))
         {
            $hashed = getHashedVerifier($verifier, $row['user_id'],
              new DateTime($row['expires']));

            if (hash_equals($hashed, bin2hex($row['verifier'])))
            {
               $user_id = (int)$row['user_id'];
               $token_id = (int)$row['token_id'];
               $username = $row['username'];
               $status = $row['status'];
            }
         }
      }

      if (!isset($token_id))
      {
         $this->add_error_message($this->passConfig->getInvalidOrExpiredText(),
            'token', $this->passConfig->getInvalidOrExpiredTag());

         return false;
      }

      return array('user_id'=>$user_id, 'username'=>$username,
        'token_id'=>$token_id, 'status'=>$status);
   }

   /**
    * Creates a password reset token and emails the user.
    *
    * @param $username the username
    * @return true if the token was successfully created and the
    * email successfully sent or false if an error occurred
    */ 
   public function createPasswordReset($username)
   {
      $user_info = $this->getUserInfo($username);

      if ($user_info === false)
      {
         $this->record_action('createPasswordReset', "No data for user '$username'", true);
         return false;
      }

      if ($user_info['status']===self::STATUS_BLOCKED)
      {
         $this->add_error_message($this->passConfig->getAccountBlockedText());

         $this->record_action('createPasswordReset', "Account blocked for '$username'", true);

         return false;
      }
      elseif ($user_info['status']===self::STATUS_PENDING)
      {
         $this->add_error_message($this->passConfig->getAccountNotVerifiedText());

         $this->record_action('createPasswordReset', "Account not verified for '$username'", true);

         return false;
      }

      $timeout = (int)$this->getConfigValue('reset_link_timeout');

      $token = $this->create_account_token((int)$user_info['id'], $timeout);

      if ($token === false)
      {
         return false;
      }

      return $this->passConfig->sendPasswordResetEmail(
         $this->getWebsite($this->getResetPasswordRef()), $token, $timeout, $username);
   }

   /**
     * Changes a password if the token is valid and emails the user
     * a notification of the action.
     *
     * Ensure that the password matches criteria first with
     * Pass::get_password_variable($name, true).
     *
     * NB this doesn't log the user in. If successful, the user will
     * need to then log in with the new password.
     *
     * @param $token the password reset token supplied by the user
     * @param $plaintextpassword the new password supplied by the
     * user
     * @return true if successful or false if token invalid/expired
     * or database error
    */
   public function passwordReset($token, $plaintextpassword)
   {
      $token_data = $this->verify_account_token($token);

      if ($token_data === false)
      {
         $this->record_action('passwordReset',
          "Password reset verification failed (token '$token' invalid or expired)", true);

         return false;
      }

      if ($token_data['status'] === self::STATUS_BLOCKED)
      {
         $this->add_error_message($this->passConfig->getAccountBlockedText());

         $this->record_action('passwordReset', 
          'Account blocked for UID ' . $token_data['user_id']);

         return false;
      }
      else if ($token_data['status'] === self::STATUS_PENDING)
      {
         $this->add_error_message($this->passConfig->getAccountNotVerifiedText());

         $this->record_action('passwordReset', 
          'Account pending verification for UID ' . $token_data['user_id']);

         return false;
      }

      $username = $token_data['username'];

      $this->record_action('passwordReset',
            sprintf("Verification successful for '%s' (UID: %d)",
              $username, $token_data['user_id']), true);

      if ($this->update_password($plaintextpassword, false, $token_data['user_id'], $username))
      {
         $this->record_action('passwordReset',
           sprintf("Update successful (%s, UID: %d)", $username, $token_data['user_id']), true);

         $this->db_query(
           sprintf("DELETE FROM tokens WHERE id=%d", $token_data['token_id']));

	 return true;
      }

      $this->record_action('passwordReset', 
         'Password update failed for UID: ' . $token_data['user_id']);

      return false;
   }

   /**
     * Checks if the user's password is valid.
     *
     * If the user has enabled 2FA and $check2fa is true then the
     * user will have to additionally provide the second
     * authentication step unless the trust cookie is set and has a
     * valid non-expired token.
     *
     * If the given user ID matches the currently logged in user,
     * and the password is only required for confirmation of an action,
     * such as changing their password, then no second verification step
     * is necessary, in which case set $check2fa to false.
     *
     * If successful, the return value is a hashed array with the
     * keys: 'id' (UID), 'username', 'role' (student, staff or admin),
     * 'status' (will always be verified), 'mfa' (true if 2FA enabled or false
     * otherwise), '2fa_key_verified' (true, if 2FA has been setup
     * and the key has been verified by the user although they may
     * have since disabled 2FA), '2fa_key' (encrypted 2FA key or null if not set),
     * 'account_creation' (timestamp the account was created),
     * 'regnum' (student registration number, or null if not set),
     * and 'requires_verification' (true, if second verification
     * step required).
     *
     * @param $user_id either be a string (user name) or an integer (user ID)
     * @param $plaintextpassword the new password supplied by the
     * user
     * @param $check2fa if true and the user has enabled 2FA, require second
     * authentication unless trust cookie is set and valid (default: true).
     * @return false if query failed or user ID doesn't exist or password doesn't match or account is pending or blocked otherwise array of user information
     */
   public function verifyCredentials($user_id, $plaintextpassword, $check2fa=true)
   {
      if (is_integer($user_id))
      {
         $sql = sprintf("SELECT id, %s, %s FROM users WHERE id=%d",
           self::SQL_SELECT_USER_FIELDS, self::SQL_SELECT_AUTH_FIELDS, $user_id);
      }
      elseif (is_string($user_id))
      {
         $sql = sprintf("SELECT id, %s, %s FROM users WHERE username='%s'",
           self::SQL_SELECT_USER_FIELDS, self::SQL_SELECT_AUTH_FIELDS,
           $this->db_escape_sql($user_id));
      }
      else
      {
         error_log("(verifyCredentials) Invalid 'user' argument " . var_export($user_id, true));
	 $this->add_error_message('Invalid user parameter.');
	 return false;
      }

      $result = $this->db_query($sql);

      if (!$result)
      {
         $this->add_error_message("Unable to query database.");

         $this->record_action('verifyCredentials', 
              "SQL query failed for user '$user_id'", true);

	 return false;
      }

      $row = mysqli_fetch_assoc($result);

      if (!isset($row))
      {
         $this->add_error_message($this->passConfig->getInvalidCredentialsText());

         $this->record_action('verifyCredentials', 
              "No such user '$user_id'", true);

	 return false;
      }
      elseif ($row['status']===self::STATUS_PENDING)
      {
         $this->add_error_message($this->passConfig->getAccountNotVerifiedText());

         $this->record_action('verifyCredentials', 
           sprintf("Account '%s' is not verified (UID: %d)",
               $row['username'], $row['id']), true);

         return false;
      }
      elseif ($row['status']==='block')
      {
         $this->add_error_message($this->passConfig->getAccountBlockedText());

         $this->record_action('verifyCredentials', 
           sprintf("Account '%s' is blocked (UID: %d)", $row['username'], $row['id']), true);

         return false;
      }
      elseif (password_verify($plaintextpassword, $row['password']))
      {
         unset($row['password']);

         $this->record_action('verifyCredentials', 
           sprintf("Password verified for '%s' (UID: %d)", $row['username'], $row['id']), true);

         $user = array(
            'id'=>$row['id'], 
            'username'=>$row['username'],
            'role'=>$row['role'],
            'status'=>$row['status'],
            'mfa' => ($row['mfa']==='on' ? true : false),
            '2fa_key_verified' => ($row['2fa_key_verified']==='true' ? true : false),
            'account_creation'=>$row['account_creation']);

         if (isset($row['regnum']))
	 {
            $user['regnum'] = $row['regnum'];
	 }

         if (!empty($row['2fa_key']))
         {
            $user['2fa_key'] = $row['2fa_key'];
         }

         if ($check2fa && $user['mfa'] && $user['2fa_key_verified'])
	 {
            if (isset($_COOKIE[self::COOKIE_NAME_TRUST]) 
                   && $_COOKIE[self::COOKIE_NAME_TRUST] !== '')
            {
               if (preg_match('/^([0-9a-f]{32})([0-9a-f]{32})$/',
                     $_COOKIE[self::COOKIE_NAME_TRUST], $matches))
               {
                  $selector = $matches[1];
                  $verifier = $matches[2];

                  // regex ensures selector is a hexadecimal string but escape it anyway
                  // id column is always an integer

                  $sql = sprintf("SELECT id, user_id, verifier, expires FROM skip_totp WHERE user_id=%d AND selector=X'%s' AND CURRENT_TIMESTAMP < expires",
                    $user['id'],
                    $this->db_escape_sql($selector));

                  $result = $this->db_query($sql);

                  if ($result)
                  {
                     $found = false;

                     if ($result->num_rows > 0)
                     {// there should only be at most one match

                         while ($row = mysqli_fetch_assoc($result))
                         {
                            $hashed = getHashedVerifier($verifier, $user['id'],
                                new DateTime($row['expires']));

                            if (hash_equals($hashed, bin2hex($row['verifier'])))
                            {
                               $found = true;
                            }
                         }
                     }

                     if ($found)
                     {
                        $user['requires_verification'] = false;

                        $this->record_action('verifyCredentials', 
                           sprintf("Trust cookie valid for '%s' (UID: %d)",
                              $user['username'], $user['id']), true);
                     }
                     else
                     {
                        $user['requires_verification'] = true;

                        // unset cookie and mark as expired

                        unset($_COOKIE[$this::COOKIE_NAME_TRUST]);
                        setcookie($this::COOKIE_NAME_TRUST, '',
                         ['expires'=>time()-3600, 'path'=>'/',
                          'domain'=>PassConfig::WEB_DOMAIN]);

                        $this->record_action('verifyCredentials', 
                           sprintf("Trust cookie invalid or expired for '%s' (UID: %d)",
                             $user['username'], $user['id']), true);
                     }
                  }
                  else
                  {
                     $this->add_generic_error();
                     $user['requires_verification'] = true;

                     $this->record_action('verifyCredentials', 
                        sprintf("Trust query failed for '%s' (UID: %d)", 
                            $user['username'], $user['id']), true);
                  }
               }
               else
               {
                  // unset cookie and mark as expired

                  unset($_COOKIE[$this::COOKIE_NAME_TRUST]);
                  setcookie($this::COOKIE_NAME_TRUST, '',
                     ['expires'=>time()-3600, 'path'=>'/',
                          'domain'=>PassConfig::WEB_DOMAIN]);

                  $user['requires_verification'] = true;

                  $this->add_error_message(
                    $this->passConfig->getInvalidCookieMFARequiredText());

                  $this->record_action('verifyCredentials', 
                     sprintf("Invalid cookie for user '%s' (UID: %d)", 
                        $user['username'], $user['id']), true);
               }
	    }
            else
            {
               $user['requires_verification'] = true;

               $this->record_action('verifyCredentials', 
                  sprintf("Initiating 2FA for user '%s' (UID: %d)",
                   $user['username'], $user['id']), true);
            }
         }
         else
         {
            $user['requires_verification'] = false;
         }

         $this->user = $user;

         return $user;
      }
      else
      {
         if (isset($this->user) && isset($this->user['id']))
         {// user logged in, but requires extra check (e.g. change password request)

            $this->record_action('verifyCredentials', 
              sprintf("Invalid password for user '%s' (UID: %d)", 
                $this->user['username'], $this->user['id']), true);

            $this->add_error_message($this->passConfig->getInvalidCredentialsText(),
             'password', $this->passConfig->getInvalidTag());
         }
         else
         {
            $this->record_action('verifyCredentials', 
              sprintf("Invalid password for user %s", $user_id), true);

            $this->add_error_message($this->passConfig->getInvalidCredentialsText());
         }

	 return false;
      }
   }

   /**
    * Checks if the given TOTP is valid.
    *
    * This function must be called before Pass::page_header to
    * allow a redirect header on failure.
    *
    * An email notification will be sent to the user if verification
    * fails.
    *
    * If the trust setting is on then, if the code is valid, the
    * trust cookie will be set to a new trust token. The trust token
    * is formed from a random 16 byte selector token and a
    * random 16 byte verifier token. The value stored in the
    * cookie is a 64-digit hexadecimal number that represents the
    * combined 32 byte token.
    *
    * @param $onecode the time-based one-time password
    * @param $trust true if trust cookie should be set if code valid
    * @return true if valid or false if user hasn't already had their password
    * verified or if they don't have a 2FA key set or if the
    * supplied code was invalid or if an error occurred
    */
   public function verifyTOTP($onecode, $trust=false)
   {
      if (!isset($this->user) || !isset($this->user['2fa_key']))
      {
         $this->add_error_message($this->passConfig->getNoMFAOrAuthFailedText());

         return false;
      }

      try
      {
         $secret = decrypt2FAkey($this->user['2fa_key']);
      }
      catch (Exception $e)
      {
         $this->add_error_message($this->passConfig->getSecretKeyDecryptFailedText());

         return false;
      }

      $ga = new PHPGangsta_GoogleAuthenticator();

      if ($ga->verifyCode($secret, $onecode))
      {
         $_SESSION['requires_verification'] = false;
         $this->user['requires_verification'] = false;

         $this->record_action('verifyTOTP', "Verification successful");

         if ($trust)
         {// trust this device for 30 days

            $selector = create_token(16);
            $verifier = create_token(16);

            $now = new DateTime();
            $expires = $now->add(new DateInterval('P30D'));

            $hashed = getHashedVerifier($verifier, $this->user['id'], $expires);

            $info = getBrowserInfo();

            $device_data = encryptDeviceData(json_encode(
               ['platform'=>htmlentities($info['os_platform']),
                'browser'=>htmlentities($info['browser']),
                'ip'=>htmlentities($_SERVER['REMOTE_ADDR'])]));

            // values aren't provided by user but escape anyway

            $sql = sprintf("INSERT INTO skip_totp SET user_id=%d, selector=X'%s', verifier=X'%s', expires='%s', device='%s'",
            $this->user['id'],
            $this->db_escape_sql($selector),
            $this->db_escape_sql($hashed),
            $this->db_escape_sql($expires->format('Y-m-d H:i:s')),
            $this->db_escape_sql($device_data));

            if ($this->db_query($sql))
            {
               setcookie($this::COOKIE_NAME_TRUST, $selector . $verifier,
                ['expires'=>$expires->getTimestamp(),
                 'path'=>'/',
                 'domain'=>PassConfig::WEB_DOMAIN,
                 'secure'=>(PassConfig::WEB_PROTOCOL === 'https'),
                 'httponly'=>true, 'samesite'=>'Strict']);

               $this->record_action('verifyTOTP', "Trust cookie set");
            }
            else
            {
               $this->record_action('verifyTOTP', "SQL trust insert failed");
            }
         }

         return true;
      }

      $info = getBrowserInfo();

      $this->passConfig->sendTotpFailedEmail($this->user['username'], $info);

      $this->logout();

      $this->add_error_message($this->passConfig->getTOTPFailedText());

      $this->record_action('verifyTOTP', "Verification failed");
 
      return false;
   }

   /**
    * Checks if the given recovery code is valid.
    *
    * The recovery code is a sequence of randomly generated bytes
    * converted to a 12-digit hexadecimal string. This is split in
    * half to form a 6-digit hexadecimal selector and verifier.
    * The selector is stored in the database table as a binary
    * object. The verifier is encrypted.
    *
    * This function must be called before Pass::page_header to
    * allow a redirect header on failure.
    *
    * An email notification will be sent to the user if verification
    * fails.
    *
    * @param $selector the selector part
    * @param $verifier the verifier part
    * @return true if valid or false otherwise
    */
   public function verifyRecoveryCode($selector, $verifier)
   {
      if (!isset($this->user))
      {
         $this->add_error_message($this->passConfig->getPasswordBeforeRecoveryText());

         return false;
      }

      $stmt = $this->db_prepare("SELECT id, verifier FROM recovery_codes WHERE user_id=? AND selector=HEX(?)");

      if ($stmt === false)
      {
         $this->add_generic_error();
         return false;
      }

      $stmt->bind_param("is", $this->user['id'], $selector);

      $code_id = null;

      if (!$stmt->execute())
      {
         $this->add_generic_error();
         $this->db_error("failed to execute statement.");
         $stmt->close();
         return false;
      }

      $stmt->bind_result($row_id, $row_verifier);

      while ($stmt->fetch())
      {
         if (!isset($code_id) &&
               hash_equals(decryptRecoveryCode($row_verifier), $verifier))
         {
            $code_id = $row_id;

            $_SESSION['requires_verification'] = false;
            $this->user['requires_verification'] = false;
         }
      }

      $stmt->close();

      if (isset($code_id))
      {
         $this->record_action('verifyRecoveryCode', "Verification successful");

         $stmt = $this->db_prepare("DELETE FROM recovery_codes WHERE id=?");

         if ($stmt === false)
         {
            $this->add_generic_error();
         }
         else
         {
            $stmt->bind_param("i", $code_id);

            if (!$stmt->execute())
            {
               $this->add_generic_error();
               $this->db_error("failed to execute delete statement");
            }

            $stmt->close();
         }

         return true;
      }

      $info = getBrowserInfo();

      $this->passConfig->sendRecoveryCodeFailedEmail($this->user['username'], $info);

      $this->logout();

      $this->add_error_message($this->passConfig->getRecoveryCodeFailedText());

      $this->record_action('verifyRecoveryCode', "Verification failed");
 
      return false;
   }

   /**
     * Gets the currently logged in user's list of trusted devices.
     *
     * The returned array will be empty if no devices have been
     * trusted or if the trust period has expired for all trusted
     * devices. A non-empty array will have a hashed array for each
     * element containing the keys 'id' (numeric trust ID),
     * 'user_id' the numeric UID (which should be identical to the
     * currently logged in UID), 'selector' (the binary selector part of
     * the token), 'verifier' (the binary verifier part of the token),
     * 'expires' (the token's expiry date) and 'device' (the device
     * name or null if unavailable).
     *
     * @return an array of trusted device information or false if an
     * error occurred or the user isn't logged in
     */ 
   public function get_trusted_devices()
   {
      if (!isset($this->user))
      {
         return false;
      }

      $result = $this->db_query(sprintf("SELECT * from skip_totp WHERE user_id = %d AND CURRENT_TIMESTAMP < expires", $this->user['id']));

      if (!$result)
      {
         return false;
      }

      $data = array();

      while ($row = $result->fetch_assoc())
      {
         array_push($data, $row);
      }

      return $data;
   }

   /**
    * Deletes the tokens for the given trusted devices from the database.
    * Note that this doesn't delete the associated cookies. The trust cookie only
    * has a 30 day lifespan so it will hopefully be deleted by the
    * browser after it expires, but it's useless without the matching
    * token in the database.
    *
    * @param $ids - either a numeric ID identifying the device or an
    * array of numeric IDs identifying the devices to remove
    * @return the number of tokens deleted or false if an error
    * occurred
    */ 
   public function remove_trusted_devices($ids)
   {
      $stmt = $this->db_prepare("DELETE FROM skip_totp WHERE id=?");

      if ($stmt === false)
      {
         $this->add_generic_error();
         return false;
      }

      $row_id = null;
      $total = 0;

      $stmt->bind_param("i", $row_id);

      if (is_array($ids))
      {
         foreach ($ids as $value)
         {
            if (is_numeric($value))
            {
               $row_id = (int)$value;

               if ($stmt->execute())
               {
                  $total++;
               }
               else
               {
                  $this->add_generic_error();
                  $this->db_error("failed to execute statement.");
               }
            }
            else
            {
               $this->add_error_message('Integer ID expected');
            }
         }
      }
      elseif (is_numeric($ids))
      {
         $row_id = (int)$ids;

         if ($stmt->execute())
         {
            $total++;
         }
         else
         {
            $this->add_generic_error();
            $this->db_error("failed to execute statement.");
         }
      }
      else
      {
         $this->add_error_message('Integer ID expected');
      }

      $stmt->close();

      return $total;
   }

   /**
    * Disables 2FA for the currently logged in user.
    * @param $delete_code true if the TOTP key should be deleted or
    * false otherwise
    */ 
   public function disable2FA($delete_code=false)
   {
      if (!isset($this->user))
      {
         return false;
      }

      $sql = "UPDATE users SET mfa='off'";

      if ($delete_code)
      {
         $sql .= ', `2fa_key`=null, `2fa_key_verified`=\'false\'';
      }

      if ($this->db_query(sprintf(
           "%s WHERE id=%d", $sql, $this->user['id'])))
      {
         $this->user['mfa'] = false;

         if ($delete_code)
         {
            unset($this->user['2fa_key']);
            $this->user['2fa_key_verified'] = false;
         }
      }
      else
      {
         $this->add_generic_error();
         return false;
      }

      return $this->passConfig->send2FADisabledEmail($this->user['username']);
   }

   /**
     * Enables 2FA for the currently logged in user. The key must
     * first be created (check with Pass::has2FAKey). If the key
     * needs verifying, supply the verification code.
     *
     * A notification email will be sent to the user.
     *
     * @param $code the TOTP code if verification is required or
     * null if the key has already been verified
     * @return true if successful or false if an error occurred
     */ 
   public function enable2FA($code=null)
   {
      if (!isset($this->user))
      {
         $this->add_error_message('No user');
         return false;
      }

      if (!isset($this->user['2fa_key']))
      {
         $this->add_error_message("TOTP key hasn't been created");
         return false;
      }

      if (isset($code))
      {
         try
         {
            $secret = decrypt2FAkey($this->user['2fa_key']);
         }
         catch (Exception $e)
         {
            $this->add_error_message(
               $this->passConfig->getSecretKeyDecryptFailedText());

            error_log($e);
            return false;
         }

         $ga = new PHPGangsta_GoogleAuthenticator();

         if ($ga->verifyCode($secret, $code))
         {
            if ($this->db_query(sprintf(
                 "UPDATE users SET mfa='on', 2fa_key_verified='true' WHERE id=%d",
                  $this->user['id'])))
            {
               $this->user['mfa'] = true;
               $this->user['2fa_key_verified'] = true;
            }
            else
            {
               $this->add_generic_error();
               return false;
            }
         }
         else
         {
            $this->add_error_message($this->passConfig->getVerificationFailedText());

            return false;
         }
      }
      elseif ($this->db_query(sprintf("UPDATE users SET mfa='on' WHERE id=%d",
              $this->user['id'])))
      {
         $this->user['mfa'] = true;
      }
      else
      {
         $this->add_generic_error();
         return false;
      }

      if ($this->user['2fa_key_verified'])
      {
         return $this->passConfig->send2FAEnabledEmail($this->user['username']);
      }
      else
      {
         return $this->passConfig->send2FAUnverifiedEmail($this->user['username']);
      }
   }

   /**
    * Creates TOTP secret key and saves it in the database and shows
    * the QR chart and key for the user to add to their
    * authenticator app.
    * @return true if key successfully created, false otherwise
    */
   public function create2FAkey()
   {
      if (!isset($this->user) || !isset($this->user['id']))
      {
         $this->add_error_message($this->passConfig->getNotAuthenticatedText());

         return false;
      }

      $ga = new PHPGangsta_GoogleAuthenticator();
      $secret = $ga->createSecret();
      $encryptedSecret = encrypt2FAkey($secret);

      if (!$this->db_query(sprintf(
           "UPDATE users SET 2fa_key_verified='false', 2fa_key='%s' WHERE id=%d",
              $this->db_escape_sql($encryptedSecret),
              $this->getUserID())
           )
         )
      {
         $this->add_generic_error();
         return false;
      }

      $this->user['2fa_key'] = $encryptedSecret;
      $this->user['2fa_key_verified'] = false;

      $qr = $this->getQRcode($secret);
?>
<div><?php $qr->printSVG(); ?></div>
<div>Key: <?php echo htmlentities($secret); ?></div>
<?php

       return true;
   }

   /**
    * Gets the QRCode object for the given TOTP secret key.
    * @param $secret the secret key
    * @return the QRCode object
    */
   private function getQRcode(string $secret)
   {
      $issuer = rawurlencode(PassConfig::WEB_TITLE);

      return QRCode::getMinimumQRCode(
              sprintf("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                 $issuer, rawurlencode($this->user['username']), 
                  rawurlencode($secret), $issuer
              ), QR_ERROR_CORRECT_LEVEL_L);
   }

   /**
     * Shows the QR chart and key for TOTP. If the key hasn't
     * already been created, Pass::create2FAkey will be used to
     * create and show the chart instead. This method is provided in case the
     * user needs to have the chart reshown. For example, they've
     * lost their authenticator app data.
     * @return true if successful, false otherwise
     */
   public function show2FAchart()
   {
      if (!isset($this->user))
      {
         return false;
      }

      if (!isset($this->user['2fa_key']))
      {
         return $this->create2FAkey();
      }
      else
      {
         $secret = decrypt2FAkey($this->user['2fa_key']);

         $qr = $this->getQRcode($secret);

?>
<div><?php $qr->printSVG(); ?></div>
<div>Key: <?php echo htmlentities($secret); ?></div>
<?php
         return true;
      }
   }

   /**
    * Gets the user's recovery codes.
    * @return an array of recovery codes (empty, if none) or false
    * if error occurs or user not logged in
    */ 
   public function get_recovery_codes()
   {
      if (!isset($this->user))
      {
         return false;
      }

      $stmt = $this->db_prepare("SELECT id, user_id, UNHEX(selector) AS unhex_selector, verifier FROM recovery_codes WHERE user_id=?");

      if ($stmt === false)
      {
         $this->add_generic_error();
         return false;
      }

      $stmt->bind_param("i", $this->user['id']);

      if (!$stmt->execute())
      {
         $this->add_generic_error();
         $this->db_error("failed to execute statement.");
         $stmt->close();
         return false;
      }

      $codes = array();

      $stmt->bind_result($id, $user_id, $selector, $verifier);

      while ($stmt->fetch())
      {
         array_push($codes, $selector . '-' . decryptRecoveryCode($verifier));
      }

      $stmt->close();

      return $codes;
   }

   /**
    * Creates a new set of recovery codes.
    * @return an array of the new codes or false if an error
    * occurred or the user isn't logged in
    */ 
   public function create_recovery_codes()
   {
      if (!isset($this->user))
      {
         return false;
      }

      $stmt = $this->db_prepare("INSERT INTO recovery_codes SET user_id=?, selector=HEX(?), verifier=?");

      if ($stmt === false)
      {
         $this->add_generic_error();
         return false;
      }

      $selector = null;
      $verifier = null;
      $encryptedVerifier = null;

      $stmt->bind_param("iss", $this->user['id'], $selector, $encryptedVerifier);

      $success = true;

      $codes = array();

      for ($i = 1; $i <= 10; $i++)
      {
         $token = create_token(6);

         $selector = substr($token, 0, 6);
         $verifier = substr($token, 6);

         array_push($codes, "$selector-$verifier");

         $encryptedVerifier = encryptRecoveryCode($verifier);

         if (!$stmt->execute())
         {
            $this->add_generic_error();
            $this->db_error("failed to execute statement.");
            $success = false;
         }
      }

      $stmt->close();

      return $codes;
   }

   /**
    * Deletes the user's recovery codes.
    * @return true if successful, false if an error occurred or the user not logged in
    */ 
   public function delete_recovery_codes()
   {
      if (!isset($this->user))
      {
         return false;
      }

      $stmt = $this->db_prepare("DELETE FROM recovery_codes WHERE user_id=?");

      if ($stmt === false)
      {
         $this->add_generic_error();
         return false;
      }

      $stmt->bind_param("i", $this->user['id']);

      $result = $stmt->execute();

      if (!$result)
      {
         $this->add_generic_error();
         $this->db_error("failed to execute statement.");
      }

      $stmt->close();

      return $result;
   }

   /**
    * Logs the user in and starts a new session.
    * Credentials must first be verified with Pass::verifyCredentials
    * (which sets $this->user if successful)
    * @return true if successful, false if user ID not set
    */
   public function login()
   {
      if (!isset($this->user['id']))
      {
         return false;
      }

      $this->clearCourseData();

      session_destroy();
      $_SESSION = array();
      session_start();
      $_SESSION['user_id'] = $this->user['id'];

      if ($this->user['requires_verification'])
      {
         $_SESSION['requires_verification'] = true;
      }
      else
      {
         $_SESSION['requires_verification'] = false;
      }

      return true;
   }

   /**
    * Log the user out and start a new session.
    */
   public function logout()
   {
      // clear all expired who's online data
      $sql = "DELETE FROM whos_online WHERE time_last_clicked < DATE_SUB(CURDATE(), INTERVAL 1 HOUR)";

      if (isset($_SESSION['user_id']) && is_numeric($_SESSION['user_id']))
      {
         $sql .= " OR user_id=" . $_SESSION['user_id'];
      }

      if (!$this->db_query($sql))
      {
         error_log("Query failed: ".$sql);
      }

      session_destroy();
      $_SESSION = array();
      session_start();

      if (isset($this->user) && isset($this->user['id']))
      {
         $this->record_action('logout', 'Session cleared');
      }

      unset($this->user);
   }

   /**
    * Clears all course and assignment session data for the current user to force a
    * reload of the XML files.
    */ 
   public function clearCourseData()
   {
      unset($_SESSION['coursedata']);
      unset($this->coursedata);
      unset($_SESSION['assignmentdata']);
      unset($this->assignmentdata);
   }

   /**
    * Creates a new user. Before calling, check the username to ensure it 
    * matches the required regular expression and that the password
    * isn't obviously insecure. See get_username_variable() and
    * get_password_variable().
    * @param $username the new username
    * @param $plaintext the plain text password
    * @return confirmation message on success, false on failure
   */
   public function create_user(string $username, string $plaintext)
   {
      $is_maintenance = ($this->getConfigValue('mode')===1 ? true : false);

      // only allow the test user's account to be created if
      // maintenance mode is on
      if (!$is_maintenance && $this->isTestUser($username))
      {
         $this->add_error_message($this->passConfig->getInvalidTestUserText());

         return false;
      }

      $hashedpassword = $this->createPasswordHash($username, $plaintext);

      if ($hashedpassword === false)
      {
         return false;
      }

      $new_user = array('username'=>$username);

      $sql_username = $this->db_escape_sql($new_user['username']);
      $sql_password = $this->db_escape_sql($hashedpassword);

      $result = $this->db_query(
         sprintf("SELECT id FROM users WHERE username='%s'", $sql_username));

      if (!$result)
      {
         $this->add_error_message("Unable to query database.");
	 return false;
      }

      if (mysqli_num_rows($result) > 0)
      {
         $this->add_error_message($this->passConfig->getUserAlreadyExistsText(
	       htmlentities($username)));

         $this->record_action('create_user', "Account '$username' already exists", true);

	 return false;
      }

      if (!$this->db_query(sprintf("INSERT INTO users SET username='%s', password='%s', status='%s'", self::STATUS_PENDING, $sql_username, $sql_password)))
      {
         $this->add_error_message("Failed to insert new user into database.");
         $this->record_action('create_user', "Insert statement failed");
         return false;
      }

      $user_id = $this->db_insert_id();

      $timeout = (int)$this->getConfigValue('verify_link_timeout');

      $token = $this->create_account_token($user_id, $timeout);

      if ($token === false)
      {
         return false;
      }

      $msg = $this->passConfig->sendAccountCreatedEmail(
        $this->getWebsite($this->getVerifyAccountRef()), $token, $timeout, $username);

      if (empty($msg))
      {
         $this->record_action('create_user', 
           "Successfully created '$username' but failed to send email", true);
      }
      else
      {
         $this->record_action('create_user', "Successfully created '$username'", true);
      }

      return $msg;
   }

   /**
    * Creates a password hash but also checks that the password
    * isn't the same as the username.
    */ 
   private function createPasswordHash(string $username,
     string $newplaintextpassword)
   {
      // check the password isn't the username
      if ($username === $newplaintextpassword)
      {
         $this->add_error_message($this->passConfig->getPasswordNotUsernameText(),
            'newpassword', $this->passConfig->getInsecureTag());

	 return false;
      }

      $hashedpassword = password_hash($newplaintextpassword, PASSWORD_DEFAULT);
      
      $hashedpassword_len = strlen($hashedpassword);

      if ($hashedpassword_len > self::MAX_HASHED_PASSWORD_LENGTH)
      {
         // If this happens then it's because the default algorithm
         // used by password_hash has changed to produce longer
         // hashes. The database column will need to be enlarged and
         // the constant updated to match.

         $this->add_error_message($this->passConfig->getPasswordHashTooLongText());

         $this->record_action('createPasswordHash',
          sprintf("Failed: hashed password too long. Hashed length: %d > %d. The password column in the database needs to be made larger and the Pass::MAX_HASHED_PASSWORD_LENGTH constant needs to be updated to match.",
             $hashedpassword_len, self::MAX_HASHED_PASSWORD_LENGTH));

         error_log("Max password hash length too small. Hash size: $hashedpassword_len. The database column needs to be made larger and update Pass::MAX_HASHED_PASSWORD_LENGTH");

         return false;
      }

      return $hashedpassword;
   }

   /**
    * Creates a new verification token and sends an email to the
    * user.
    * @param $username the username
    * @return confirmation message if successful or false if an
    * error occurred
    */ 
   public function resend_verification_email(string $username)
   {
      $stmt = $this->db_prepare("SELECT id, status FROM users WHERE username=?");

      if ($stmt === false)
      {
         $this->add_generic_error();
         $this->record_action('resend_verification_email',
           "SQL select failed ($username)", true);
         return false;
      }

      $stmt->bind_param("s", $username);

      if (!$stmt->execute())
      {
         $this->add_generic_error();
         $this->db_error("failed to execute statement.");
         $this->record_action('resend_verification_email',
           "SQL execute failed ($username)", true);
         $stmt->close();
         return false;
      }

      $row_id = null;
      $row_status = null;

      $stmt->bind_result($row_id, $row_status);

      // username column is unique so there should only be 0 or 1 rows

      $stmt->fetch();

      $stmt->close();

      if (!isset($row_id))
      {
         $this->add_error_message($this->passConfig->getNoAccountText(
           htmlentities($username)));

         $this->record_action('resend_verification_email',
           "No such user '$username'", true);

         return false;
      }

      if ($row_status === self::STATUS_BLOCKED)
      {
         $this->add_error_message($this->passConfig->getAccountBlockedText());

         $this->record_action('resend_verification_email',
           "Account '$username' blocked", true);

         return false;
      }
      elseif ($row_status === self::STATUS_ACTIVE)
      {
         $this->add_error_message($this->passConfig->getAccountAlreadyVerifiedText());

         $this->record_action('resend_verification_email',
           "Account '$username' already verified", true);

         $this->passConfig->sendAlreadyVerifiedEmail($username);

         return false;
      }

      $timeout = (int)$this->getConfigValue('verify_link_timeout');

      $token = $this->create_account_token($row_id, $timeout);

      if ($token === false)
      {
         return false;
      }

      $msg = $this->passConfig->sendVerificationEmail(
        $this->getWebsite($this->getVerifyAccountRef()), $token, $timeout, $username);

      if (empty($msg))
      {
          $this->record_action('resend_verification_email',
             "Failed to send verification to '$username'", true);
      }
      else
      {
          $this->record_action('resend_verification_email',
             "Verification sent to '$username'", true);
      }

      return $msg;
   }

   /**
    * Marks the account as verified if the given token is valid.
    * @param $token the verification token
    * @return true if successful or false if token invalid or error occurs
    */ 
   public function verify_account(string $token)
   {
      $token_data = $this->verify_account_token($token);

      if ($token_data === false)
      {
         $this->record_action('verify_account',
          "Account verification failed (token '$token' invalid or expired)", true);

         return false;
      }

      if ($token_data['status'] === self::STATUS_BLOCKED)
      {
         $this->add_error_message($this->passConfig->getAccountBlockedText());

         $this->record_action('verify_account', 
          'Account blocked for UID ' . $token_data['user_id']);

         return false;
      }
      else if ($token_data['status'] === self::STATUS_ACTIVE)
      {
         $this->add_error_message($this->passConfig->getAccountAlreadyVerifiedText());

         $this->record_action('verify_account', 
          'Account already verified for UID ' . $token_data['user_id']);

         return false;
      }

      $user_id = (int)$token_data['user_id'];
      $username = $token_data['username'];

      if (!$this->db_query(sprintf("UPDATE users SET status='%s' WHERE id=%d",
            self::STATUS_ACTIVE, $user_id)))
      {
         $this->add_generic_error();
         $this->record_action('verify_account',
            "SQL update failed for '$username' (UID: $user_id)", true);

         return false;
      }

      $this->record_action('verify_account',
          "Account verified for '$username' (UID: $user_id)", true);

      if (!$this->db_query(
            sprintf("DELETE FROM tokens WHERE id=%d", $token_data['token_id'])))
      {
         $this->add_generic_error();
      }

      return true;
   }

   /**
    * Updates a user's password.
    * Verify credentials or token before calling this see also
    * verify_account_token($token) and
    * get_password_variable($name, true).
    * The user won't be logged in for password changes via reset token.
    * @param $newplaintextpassword the new password
    * @param $check_logged_in if true, this function will fail if
    * the user isn't logged in. If this is set to false, make sure
    * that the token has been verified first.
    * @param $user_id the user's ID: null if user logged in or
    * the user's ID if the token has been verified 
    * @param $username the username to send the confirmation email
    * to (null for currently logged in user)
    * @return confirmation message if successful or false otherwise 
    */
   public function update_password($newplaintextpassword, $check_logged_in=true,
    $user_id=null, $username=null)
   {
      if (!isset($user_id) && isset($this->user['id']))
      {
         $user_id = $this->user['id'];
      }

      if ($check_logged_in && !isset($user_id))
      {
          $this->add_error_message($this->passConfig->getNotLoggedInText());

          return false;
      }

      if (!isset($username))
      {
         $username = $this->getUserName();
      }

      $hashedpassword = $this->createPasswordHash($username, $newplaintextpassword);

      if ($hashedpassword === false)
      {
         return false;
      }

      $sql = sprintf("UPDATE users SET password='%s' WHERE id=%d",
         $this->db_escape_sql($hashedpassword), $user_id);

      if ($this->db_query($sql))
      {
         $this->record_action('update_password',
            "Password updated (UID: $user_id)");

         return $this->passConfig->sendPasswordUpdatedEmail($username);
      }
      else
      {
         $pass->add_error_message("Failed to insert password into database.");

         $this->record_action('update_password',
            "SQL update failed (UID: $user_id)");

         return false;
      }
   }

   /**
    * Updates user's own registration number.
    * @param $regnum the registration number (null or empty to unset)
    * @return true if successful or false otherwise
    */
   public function setUserRegNum($regnum)
   {
      if (!isset($this->user['id']))
      {
         $this->add_error_message('User ID not set.');
      }

      if (empty($regnum))
      {
         $sql = sprintf("UPDATE users SET regnum=NULL WHERE id=%d", $this->user['id']);
      }
      else
      {
         $sqlregnum = $this->db_escape_sql($regnum);

         $sql = sprintf("SELECT id, regnum FROM users WHERE regnum='%s' AND id<>%d", $sqlregnum, $this->user['id']);

         $result = $this->db_query($sql);

         if ($result)
         {
            if (mysqli_num_rows($result) > 0)
            {
               $row = $result->fetch_assoc();

               $this->add_error_message($this->passConfig->getRegNumClashText(
                htmlentities($regnum)));

               $this->record_action('setUserRegNum',
                "Failed to set reg num to '$regnum' (already assigned to UID: "
                    . $row['id'] . ")", true);

               return false;
            }
         }
         else
         {
            $this->record_action('setUserRegNum', 'SQL select failed');

            $this->add_generic_error();
            return false;
         }

	 $sql = sprintf("UPDATE users SET regnum='%s' WHERE id=%d",
            $sqlregnum, $this->user['id']);
      }

      if ($this->db_query($sql))
      {
         if (empty($regnum))
         {
            $this->record_action('setUserRegNum',
              "Successfully unset reg num");

	    $this->user['regnum'] = null;
         }
         else
         {
            $this->record_action('setUserRegNum',
              "Successfully set reg num to '$regnum'", true);

	    $this->user['regnum'] = $regnum;
         }

         return true;
      }

      $this->record_action('setUserRegNum',
           "SQL update failed to set reg num to '$regnum'", true);

      $this->add_error_message('Failed to update database entry.');
      return false;
   }

   /**
    * Updates other user's details (admin only).
    * The details are supplied as an array that must have 'id' set
    * to the user's ID. Other optional keys: 'username' (new
    * username) and 'originalusername' (old username),
    * 'role' (new role) and 'originalrole' (old role), 'status'
    * (new status) and 'originalstatus' (old status), 'regnum' (new
    * registration number) and 'originalregnum' (old registration
    * number). For each new value, if it's the same as the old
    * value, then no change is made. If the old value is missing or
    * empty (which should only be for the registration number), then
    * it's being set for the first time. If the old value is
    * different to the new value then a change needs to be made.
    *
    * @param $user array containing user details. If null, use the
    * array containing the page parameters.
    * @param $send_email true if confirmation email should be sent
    * to the user
    * @param $error_on_no_change if true, consider no update
    * information to be an error, otherwise silently return true if
    * no update
    * @return true if successful or false otherwise
    */
   public function updateUser($user=null, $send_email=true, $error_on_no_change=true)
   {
      if (!$this->isUserRole(self::ROLE_ADMIN))
      {
         $this->add_error_message('Admin required to update user details.');
	 return false;
      }

      $newusername = null;
      $newrole = null;
      $newstatus = null;
      $newregnum = null;

      if (empty($user))
      {
         $user = $this->params;
      }

      if (!is_numeric($user['id']))
      {
         $this->add_error_message('User ID must be an integer');
	 return false;
      }

      $set_values = '';

      if (!empty($user['username']) && (!isset($user['originalusername'])
            || $user['originalusername'] !== $user['username']))
      {
         $set_values = sprintf("username='%s'",
	    $this->db_escape_sql($user['username']));

         $newusername = $user['username'];
      }

      if (!empty($user['role']) && (!isset($user['originalrole'])
            || $user['originalrole'] !== $user['role']))
      {
         if (!empty($set_values))
	 {
            $set_values .= ', ';
	 }

         $set_values .= sprintf("role='%s'", $this->db_escape_sql($user['role']));

         $newrole = $user['role'];
      }

      if (!empty($user['status']) && (!isset($user['originalstatus'])
            || $user['originalstatus'] !== $user['status']))
      {
         if (!empty($set_values))
	 {
            $set_values .= ', ';
	 }

         $set_values .= sprintf("status='%s'", $this->db_escape_sql($user['status']));

         $newstatus = $user['status'];
      }

      if (!isset($user['originalregnum'])
	      || $user['originalregnum'] !== $user['regnum'])
      {
         if (!empty($set_values))
	 {
            $set_values .= ', ';
	 }

         if (empty($user['regnum']))
         {
            $set_values .= 'regnum=NULL';

            $newregnum = '';
         }
         else
         {
            $set_values .= sprintf("regnum='%s'", 
	       $this->db_escape_sql($user['regnum']));

            $newregnum = $user['regnum'];
         }
      }

      if (empty($set_values))
      {
         if ($error_on_no_change)
         {
            $this->add_error_message("No new user information to update.");
	    return false;
         }
         else
         {
            return true;
         }
      }

      $sql = sprintf("UPDATE users SET %s WHERE id=%d", $set_values, $user['id']);

      $result = $this->db_query($sql);

      if (!$result)
      {
         $this->record_action('updateUser',
           sprintf("SQL update failed change records for UID: %d (%s)",
             $user['id'], $set_values), true);

         return false;
      }

      $this->record_action('updateUser',
           sprintf("Updated user data for UID: %d (%s)", $user['id'], $set_values), true);

      if ($this->getUserID() === $user['id'])
      {
         $this->user['regnum'] = $user['regnum'];
         $this->user['username'] = $user['username'];

	 if (!empty($user['role']))
	 {
            $this->user['role'] = $user['role'];
	 }
      }

      if ($send_email)
      {
         $this->passConfig->sendAdminUpdatedDetails($user['username'],
          $newusername, $newrole, $newstatus, $newregnum);
      }

      return $result;
   }

   /**
    * Clears invalid registration number and notify user (admin only).
    * Used when a student has supplied the wrong registration number. 
    * Students can't upload if their registration number isn't set, so
    * this will force them to update it when they next try to upload
    * their files. (Don't guarantee that they won't supply the wrong
    * value again!) An email will be sent notifying the user of the
    * change. If the old invalid registration number is supplied,
    * that will be included in the email.
    *
    * @param $userid the UID of the user that needs to have their
    * registration number cleared
    * @param $username the username (to send the email notification)
    * @param $invalidregnum the old invalid registration number (may
    * be null)
    */
   public function invalidateRegNum(int $userid, string $username, 
      $invalidregnum=null)
   {
      if (!$this->isUserRole(self::ROLE_ADMIN))
      {
         $this->add_error_message('Admin required to alter user data.');
	 return false;
      }

      $sql = "UPDATE users SET regnum=NULL WHERE id=$userid";

      $result = $this->db_query($sql);

      if (!$result)
      {
         $this->record_action('invalidateRegNum',
           "SQL update failed to unset reg num for UID: $userid");

         return false;
      }

      $this->record_action('invalidateRegNum',
        "Unset reg num for UID: $userid");

      return $this->passConfig->sendAdminClearedRegNum($username, $invalidregnum);
   }

   /**
    * Merges two user accounts (admin only).
    * For use when the same user has inadvertently created a
    * duplicate account. This will copy over the submission data of
    * the duplicate account. This shouldn't happen now that
    * account verification is required. If no submission data in the duplicate
    * account, simply delete it.
    * @param $user array of information about both accounts: 'id'
    * (correct UID) and 'id2' (duplicate account's UID),
    * 'username' (correct username) and 'username2' (incorrect
    * username).
    * @param $send_email if true send email notification 
    * @return array of messages (which may include error messages)
    * or false if internal error occurred
    */
   public function mergeUsers($user=null, $send_email=true)
   {
      if (!$this->isUserRole(self::ROLE_ADMIN))
      {
         $this->add_error_message('Admin required to merge users.');
	 return false;
      }

      if (!isset($user))
      {
         $user = $this->params;
      }

      if (!isset($user['id']) || !is_numeric($user['id']))
      {
         $this->add_error_message('id not set or not numeric.');
	 return false;
      }

      if (!isset($user['id2']) || !is_numeric($user['id2']))
      {
         $this->add_error_message('id2 not set or not numeric.');
	 return false;
      }

      if (!isset($user['username']))
      {
         $this->add_error_message('username not set.');
	 return false;
      }

      if (isset($user['username2']))
      {
         $username2 = $user['username2'];
      }
      else
      {
         $username2 = $user['username'];
      }

      $id = (int)$user['id'];
      $id2 = (int)$user['id2'];

      if ($id === $id2)
      {
         $this->add_error_message("Can't merge user with themself");
         return false;
      }

      $info = array();

      // change all references to $id2 to $id

      if ($this->db_query(
        sprintf("UPDATE submissions SET uploaded_by=%d WHERE uploaded_by=%d",
          $id, $id2)))
      {
         $n = $this->db_affected_rows();
         array_push($info,
          sprintf("Updated %d %s.", $n, $n===1 ?  'submission' : 'submissions'));
      }
      else
      {
         $this->add_error_message("Failed to update submissions");
      }

      if ($this->db_query(
        sprintf("UPDATE projectgroup SET user_id=%d WHERE user_id=%d",
          $id, $id2)))
      {
         $n = $this->db_affected_rows();
         array_push($info, 
            sprintf("Updated %d project %s.", $n, $n===1 ?  'group' : 'groups'));
      }
      else
      {
         $this->add_error_message("Failed to update projectgroup");
      }

      if ($this->db_query(
        sprintf("UPDATE action_recorder SET user_id=%d WHERE user_id=%d",
          $id, $id2)))
      {
         $n = $this->db_affected_rows();
         array_push($info,
           sprintf("Updated %d %s.", $n, $n===1 ?  'action' : 'actions'));
      }
      else
      {
         $this->add_error_message("Failed to update action_recorder");
      }

      if ($this->db_query(
        sprintf("UPDATE tokens SET user_id=%d WHERE user_id=%d",
          $id, $id2)))
      {
         $n = $this->db_affected_rows();
         array_push($info,
           sprintf("Updated %d %s. ", $n, $n===1 ?  'token' : 'tokens'));
      }
      else
      {
         $this->add_error_message("Failed to update tokens");
      }

      if ($this->has_errors())
      {
         $this->record_action('mergeUsers', "Failed to merge UID: $id and UID: $id2");
      }
      else
      {
         $this->record_action('mergeUsers', "Merged UID: $id and UID: $id2");
      }

      if ($this->updateUser($user, false, false))
      {
         array_push($info, "Updated user $id data.");
      }

      if ($this->has_errors())
      {
         array_push($info, "One or more errors has occurred. Not deleting user $id2.");
         return $info;
      }

      if ($this->deleteUser($id2))
      {
         array_push($info, "Deleted user $id2.");

         if ($send_email)
         {
            if ($this->passConfig->sendAdminMergedAccounts($user['username'], $username2))
            {
               array_push($info, 'Email sent.');
            }
         }
      }

      return $info;
   }

   /**
    * Deletes user details from database (admin only).
    * This doesn't delete uploaded files or result files.
    * @param $userid the UID of the user to be removed
    * @return true if successful or false otherwise
    */
   public function deleteUser(int $userid)
   {
      if (!$this->isUserRole(self::ROLE_ADMIN))
      {
         $this->add_error_message('Admin required to delete user from database.');
	 return false;
      }

      if ($userid === $this->getUserID())
      {
         $this->add_error_message("Can't delete yourself");
	 return false;
      }

      $success = true;

      if (!$this->db_query("DELETE FROM users WHERE id=$userid"))
      {
         $this->add_error_message("Failed to delete row from users.");
         $success = false;
      }

      if (!$this->db_query("DELETE FROM submissions WHERE uploaded_by=$userid"))
      {
         $this->add_error_message("Failed to delete row from submissions.");
         $success = false;
      }

      if (!$this->db_query("DELETE FROM projectgroup WHERE user_id=$userid"))
      {
         $this->add_error_message("Failed to delete row from projectgroup.");
         $success = false;
      }

      if (!$this->db_query("DELETE FROM tokens WHERE user_id=$userid"))
      {
         $this->add_error_message("Failed to delete row from tokens.");
         $success = false;
      }

      if (!$this->db_query("DELETE FROM whos_online WHERE user_id=$userid"))
      {
         $this->add_error_message("Failed to delete row from whos_online.");
         $success = false;
      }

      if (!$this->db_query("DELETE FROM action_recorder WHERE user_id=$userid"))
      {
         $this->add_error_message("Failed to delete row from action_recorder.");
         $success = false;
      }

      if (!$this->db_query("DELETE FROM recovery_codes WHERE user_id=$userid"))
      {
         $this->add_error_message("Failed to delete row from recovery_codes.");
         $success = false;
      }

      if (!$this->db_query("DELETE FROM skip_totp WHERE user_id=$userid"))
      {
         $this->add_error_message("Failed to delete row from skip_totp.");
         $success = false;
      }

      if ($success)
      {
         $this->record_action('deleteUser', "Deleted user $userid");
      }
      else
      {
         $this->record_action('deleteUser', "Failed to delete user $userid");
      }

      return $success;
   }

   /**
    * Gets the role of the current user.
    * @return one of:
    * <ul>
    *  <li> false : not logged in;
    *  <li> 'student' : allow uploads and view their own upload
    *  data;
    *  <li> 'staff': as student but can also view all uploaded data,
    *  export final upload data, and view additional FAQ content;
    *  <li> 'admin': as staff but can also edit other users' data
    * (such as changing the user role), delete data (including
    * upload directories), requeue submissions, change the frontend
    * configuration, and view backend maintenance information.
    * </ul>
   */
   public function getUserRole()
   {
      if (!isset($this->user) || !isset($this->user['role'])) return false;

      return $this->user['role'];
   }

   /**
    * Checks if the current user has the given role.
    * @param $role may be any of the return values of getUserRole()
    * @return true if current user's role matches the given role
    */ 
   public function isUserRole($role)
   {
      if (!isset($this->user) || !isset($this->user['role']))
      {
         return $role === false ? true : false;
      }

      return $this->user['role'] === $role ? true : false;
   }

   /**
    * Checks if the current user has the given status.
    * @param $status false for not logged in or 'pending' for
    * unverified account, 'active' for verified account or
    * 'blocked' for a blocked account
    * @return true if the current user's status matches the given
    * status
    */ 
   public function isUserStatus($status)
   {
      if (!isset($this->user) || !isset($this->user['status']))
      {
         return $status === false ? true : false;
      }

      return $this->user['status'] === $status ? true : false;
   }

   /**
    * Checks is the current user is either staff or admin.
    * @return true if current user is logged in and has the role
    * 'staff' or 'admin', false otherwise
    */ 
   public function isUserStaffOrAdmin()
   {
      return isset($this->user) && isset($this->user['role'])
         && ($this->user['role'] === self::ROLE_STAFF
              || $this->user['role'] === self::ROLE_ADMIN) ?
         true : false;
   }

   /**
    * Checks if current user is authenticated. This means that the
    * user needs to have successfully entered their password and
    * their account isn't blocked and they have either passed the 2FA
    * step or don't have 2FA enabled. If the 2FA step is still
    * pending, this will return false.
    * @return true if user authenticated or false otherwise
    */ 
   public function isUserAuthenticated()
   {
      if (!isset($this->user) || !isset($this->user['role'])
           || $this->user['status']===self::STATUS_BLOCKED)
      {
         return false;
      }

      return !isset($this->user['requires_verification']) || $this->user['requires_verification'] === false;
   }

   /**
    * Checks if second verification is required.
    * @return true if user is logged in and has 2FA enabled but has
    * not yet completed the 2FA step
    */ 
   public function isUserVerificationRequired()
   {
      return isset($this->user) && isset($this->user['requires_verification']) && $this->user['requires_verification'];
   }

   /**
    * Checks if user has 2FA enabled.
    * @return true if user is logged in (but possibly hasn't completed
    * the second step) and has 2FA enable or false otherwise
    */ 
   public function is2FAEnabled()
   {
      if (!isset($this->user) || !isset($this->user['mfa'])) return false;

      return $this->user['mfa'] && $this->user['2fa_key_verified'];
   }

   /**
    * Checks if the user has verified their 2FA key.
    * @return true if the user is logged in and has 
    * verified a 2FA key, false otherwise.
    */ 
   public function is2FAVerified()
   {
      if (!isset($this->user) || !isset($this->user['2fa_key_verified'])) return false;

      return $this->user['2fa_key_verified'];
   }

   /**
    * Checks if the user has created a 2FA key.
    * @return true if the user is logged in and has created 
    * a 2FA key, false otherwise.
    */ 
   public function has2FAKey()
   {
      if (!isset($this->user) || !isset($this->user['2fa_key'])) return false;

      return isset($this->user['2fa_key']);
   }

   /**
    * Gets the current user's numeric ID.
    * @return the current user's UID (int) if they are logged in or
    * false otherwise
    */ 
   public function getUserID()
   {
      if (!isset($this->user) || !isset($this->user['id'])) return false;

      return (int)$this->user['id'];
   }

   /**
    * Gets the current user's username.
    * @return the username if the user is logged in or
    * false otherwise
    */ 
   public function getUserName()
   {
      if (!isset($this->user) || !isset($this->user['username'])) return false;

      return $this->user['username'];
   }

   /**
    * Checks if the username is the test user.
    * @param $username the username to test or null to test the
    * currently logged in username
    * @return true if the username matches the test account username
    */ 
   public function isTestUser($username=null)
   {
      if (!isset($username))
      {
         if (!isset($this->user) || !isset($this->user['username'])) return false;

         $username = $this->user['username'];
      }

      return $username === $this->getConfigValue('test_user');
   }

   /**
    * Checks if the given email address is the test user's email
    * address. The test user's email address is the (non-existent)
    * address formed from the test username and the domain name,
    * not the actual email address that will be used by send_email.
    *
    * This function is used by processEmailAddress to determine
    * whether or not the email address needs to be substituted.
    *
    * @param $email the email address to test
    * @return true if the supplied email is not null and equals the
    * test user's email address, false otherwise
    */ 
   public function isTestUserEmail($email)
   {
      if (!isset($email)) return false;

      return $email === ($this->getEmailAddress($this->getConfigValue('test_user')));
   }

   /**
    * Gets the real email address to use instead of the non-existent
    * test user's email address.
    * @return the real email address for the test user account
    */ 
   public function getTesterEmail()
   {
      return $this->getEmailAddress($this->getConfigValue('test_user_redirect'));
   }

   /**
    * Gets the actual email address to use. This function is used by
    * send_email to substitute the test user account's non-existent
    * email address with the tester's real email address.
    * @param $email the email address to test
    * @return either the original email address (if it's not the
    * test account's email address) or the tester's email address
    * otherwise
    */ 
   public function processEmailAddress($email)
   {
      if ($this->isTestUserEmail($email))
      {
         return $this->getTesterEmail();
      }
      else
      {
         return $email;
      }
   }

   /**
    * Gets the current user's registration number.
    * @return false if not logged in or not set, otherwise the user's registration
    * number
    */ 
   public function getUserRegNum()
   {
      if (!isset($this->user) || !isset($this->user['regnum'])) return false;

      return $this->user['regnum'];
   }

   /**
    * Gets the email address associated with the given username.
    * @param $username the username
    * @return the user's email address
    */ 
   public function getEmailAddress(string $username)
   {
      return $this->passConfig->getEmailAddress($username);
   }

   /**
    * Gets the email address of the current user.
    * @return the user's email address or false if not logged in
    */ 
   public function getUserEmail()
   {
      $username = $this->getUserName();

      if (!$username) return false;

      return $this->getEmailAddress($username);
   }

   /**
    * Gets the full URL of a path on this website.
    * @param $path null for the homepage or the path element of the
    * URL (starting with '/')
    * @return the URL
    */ 
   public function getWebsite($path=null)
   {
      return $this->passConfig->getWebsiteUrl($path);
   }

   /**
    * Gets the title for the (student) upload list page.
    * @return 'My Uploads' page title
    */ 
   public function getUploadListTitle()
   {
      return $this->passConfig->getUploadListTitle();
   }

   /**
    * Gets the PASS short title.
    * @return 'PASS' title
    */ 
   public function getShortTitle()
   {
      return $this->passConfig->getShortTitle();
   }

   /**
    * Gets the PASS long title.
    * @return 'Preparing Programming Assignments for Submission System' title
    */ 
   public function getLongTitle()
   {
      return $this->passConfig->getLongTitle();
   }

   /**
    * Gets the page title.
    * @return the page title or null if not set
    */ 
   public function getPageTitle()
   {
      return $this->pageTitle;
   }

   /**
    * Gets the HTML text for the "Next" button.
    * @return the "Next &#x23F5;" HTML text
    */ 
   public function getNextLabel()
   {
      return $this->passConfig->getNextLabel();
   }

   /**
    * Gets the HTML text for the "Previous" button.
    * @return the "&#x23F4; Previous" HTML text
    */ 
   public function getPrevLabel()
   {
      return $this->passConfig->getPrevLabel();
   }

   /**
    * Gets the client-side pattern for the username.
    */ 
   public function getUserNamePattern()
   {
      return $this->passConfig->getUserNamePattern();
   }

   /**
    * Gets the client-side pattern for the registration number.
    */ 
   public function getRegNumPattern()
   {
      return $this->passConfig->getRegNumPattern();
   }

   /**
    * Gets the path to the website's about page.
    */ 
   public function getAboutRef()
   {
      return $this->passConfig->getAboutRef();
   }

   /**
    * Gets the path to the website's terms page.
    */ 
   public function getTermsRef()
   {
      return $this->passConfig->getTermsRef();
   }

   /**
    * Gets the URL to the university's legal page.
    */ 
   public function getLegalRef()
   {
      return $this->passConfig->getLegalRef();
   }

   /**
    * Gets the path to the login page.
    */ 
   public function getLoginRef()
   {
      return $this->passConfig->getLoginRef();
   }

   /**
    * Gets the path to the create account page.
    */ 
   public function getCreateAccountRef()
   {
      return $this->passConfig->getCreateAccountRef();
   }

   /**
    * Gets the path to the user's account details page.
    */ 
   public function getAccountRef()
   {
      return $this->passConfig->getAccountRef();
   }

   /**
    * Gets the path to the set registration number page.
    */ 
   public function getSetRegNumRef()
   {
      return $this->passConfig->getSetRegNumRef();
   }

   /**
    * Gets the path to the change password page.
    */ 
   public function getChangePasswordRef()
   {
      return $this->passConfig->getChangePasswordRef();
   }

   /**
    * Gets the path to the reset password page.
    */ 
   public function getResetPasswordRef()
   {
      return PassConfig::RESET_PASSWORD_HREF;
   }

   /**
    * Gets the path to the forgotten password page.
    */ 
   public function getForgottenPasswordRef()
   {
      return $this->passConfig->getForgottenPasswordRef();
   }

   /**
    * Gets the path to the resend verification page.
    */ 
   public function getResendVerifyRef()
   {
      return $this->passConfig->getResendVerifyRef();
   }

   /**
    * Gets the path to the verify account page.
    */ 
   public function getVerifyAccountRef()
   {
      return $this->passConfig->getVerifyAccountRef();
   }

   /**
    * Gets the path to the second step authentication page.
    */ 
   public function getCheckMultiFactorRef()
   {
      return $this->passConfig->getCheckMultiFactorRef();
   }

   /**
    * Gets the path to the logout page.
    */ 
   public function getLogoutRef()
   {
      return $this->passConfig->getLogoutRef();
   }

   /**
    * Gets the path to the upload page (requires JavaScript).
    */ 
   public function getUploadRef()
   {
      return $this->passConfig->getUploadRef();
   }

   /**
    * Gets the path to the fallback upload page (no JavaScript).
    */ 
   public function getUploadFallbackRef()
   {
      return $this->passConfig->getUploadFallbackRef();
   }

   /**
    * Gets the path to the upload lists page.
    */ 
   public function getUploadListsRef()
   {
      return $this->passConfig->getUploadListsRef();
   }

   /**
    * Gets the path to the final upload lists page.
    */ 
   public function getFinalUploadListsRef()
   {
      return $this->passConfig->getFinalUploadListsRef();
   }

   /**
    * Gets the path to the download page.
    */ 
   public function getDownloadRef()
   {
      return PassConfig::DOWNLOAD_HREF;
   }

   /**
    * Gets the path to the FAQ page.
    */ 
   public function getFaqRef()
   {
      return $this->passConfig->getFaqRef();
   }

   /**
    * Gets the path to the admin sub-directory.
    */ 
   public function getAdminRef()
   {
      return $this->passConfig->getAdminRef();
   }

   /**
    * Gets the path to the admin configuration page.
    */ 
   public function getAdminConfigRef()
   {
      return $this->passConfig->getAdminConfigRef();
   }

   /**
    * Gets the path to the admin upload directories page.
    */ 
   public function getAdminUploadDirsRef()
   {
      return $this->passConfig->getAdminUploadDirsRef();
   }

   /**
    * Gets the path to the admin users page.
    */ 
   public function getAdminUsersRef()
   {
      return $this->passConfig->getAdminUsersRef();
   }

   /**
    * Gets the path to the admin PHP info page.
    */ 
   public function getAdminPhpInfoRef()
   {
      return $this->passConfig->getAdminPhpInfoRef();
   }

   /**
    * Gets the path to the admin view process logs page.
    */ 
   public function getAdminViewLogsRef()
   {
      return $this->passConfig->getAdminViewLogsRef();
   }

   /**
    * Gets the path to the admin who's online page.
    */ 
   public function getAdminWhosOnlineRef()
   {
      return $this->passConfig->getAdminWhosOnlineRef();
   }

   /**
    * Gets the path to the admin session data page.
    */ 
   public function getAdminSessionsRef()
   {
      return $this->passConfig->getAdminSessionsRef();
   }

   /**
    * Gets the path to the admin view recorder page.
    */ 
   public function getAdminViewActionRecorderRef()
   {
      return $this->passConfig->getAdminViewActionRecorderRef();
   }

   /**
    * Gets the path to the admin backend maintenance page.
    */ 
   public function getAdminMaintenanceRef()
   {
      return $this->passConfig->getAdminMaintenanceRef();
   }

   /**
    * Gets the path to the SSH instructions for the admin backend maintenance page.
    */ 
   public function getSshInstructions()
   {
      return $this->passConfig->getSshInstructions();
   }

   /**
    * Gets the path to the SFTP instructions for the admin backend maintenance page.
    */ 
   public function getSftpInstructions()
   {
      return $this->passConfig->getSftpInstructions();
   }

   /**
    * Gets the resourcefile source example for the staff FAQ.
    */ 
   public function getExampleSrc()
   {
      return $this->passConfig->getExampleSrc();
   }

   /**
    * Gets the example username for create account and forgotten
    * password pages.
    */ 
   public function getExampleUserName()
   {
      return $this->passConfig->getExampleUserName();
   }

   /**
    * Gets the path to the Server Pass download for the admin backend maintenance page.
    */ 
   public function getPassServerCliDownloadLink()
   {
      return $this->passConfig->getPassServerCliDownloadLink();
   }

   /**
    * Gets the file permissions to use when creating the upload directories.
    */ 
   public function getUploadPathPermissions()
   {
      return PassConfig::UPLOAD_DIR_PERMISSIONS;
   }

   /**
    * Gets the file permissions to use for uploaded files.
    */ 
   public function getUploadFilePermissions()
   {
      return PassConfig::UPLOAD_FILE_PERMISSIONS;
   }

   /**
    * Gets the path to put the upload directories in.
    */ 
   public function getUploadPath()
   {
      return PassConfig::UPLOAD_PATH;
   }

   /**
    * Gets the path to the passdocker directory.
    */ 
   public function getDockerPath()
   {
      return PassConfig::DOCKER_PATH;
   }

   /**
    * Gets the path to the passdocker/completed directory.
    */ 
   public function getCompletedPath()
   {
      return PassConfig::COMPLETED_PATH;
   }

   /**
    * Writes the header to redirect to the login page. Can't be used
    * after any content has been written to STDOUT.
    * @param $add_from true if the current script should be added as
    * the 'from' parameter
    */ 
   public function redirect_login_header($add_from=true)
   {
      $this->redirect_header($this->getLoginRef(), $add_from);
   }

   /**
    * Writes the header to redirect to the given page. Can't be used
    * after any content has been written to STDOUT.
    * @param $to the path to redirect to
    * @param $add_from true if the current script should be added as
    * the 'from' parameter
    */ 
   public function redirect_header($to='/index.php', $add_from=false)
   {
      if ($add_from)
      {
         header("Location: $to?from=".urlencode($_SERVER['PHP_SELF']));
      }
      else
      {
         header("Location: $to");
      }
   }

   /**
    * Writes the blurb used on the (unauthenticated) home page and at the start of the
    * upload pages.
    */ 
   public function printBlurb()
   {
      $this->passConfig->printBlurb();
   }

   /**
    * Gets HTML code to link to the current script.
    * @param $query query string to add (may be null or empty)
    * @param $text the link text
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function href_self($query, $text, $attrs=null)
   {
      if (empty($query))
      {
         $href = $_SERVER['PHP_SELF'];
      }
      else
      {
         $href = $_SERVER['PHP_SELF'] . "?$query";
      }

      if (!isset($attrs))
      {
         $attrs = array('href'=>$href);
      }
      elseif (is_array($attrs))
      {
         $attrs['href'] = $href;
      }
      else
      {
         $attrs .= " href=\"$href\"";
      }
 
      return html_element('a', $text, $attrs);
   }

   /**
    * Gets HTML code to link to the about page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_about_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getAboutTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getAboutRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the terms page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_terms_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getTermsTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getTermsRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the university's legal page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_legal_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getLegalTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getLegalRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the login page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_login_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getLoginTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getLoginRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the resend verification page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_resend_verify_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getResendVerifyTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getResendVerifyRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the verify account page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_verify_account_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getVerifyAccountTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getVerifyAccountRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the set registration number page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_set_regnum_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getSetRegNumTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getSetRegNumRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the create account page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_create_new_account_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getCreateAccountTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getCreateAccountRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the 'my account' page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_my_account_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getMyAccountTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getAccountRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the change password page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_change_password_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getChangePasswordTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getChangePasswordRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the forgotten password page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_forgotten_password_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getForgottenPasswordTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getForgottenPasswordRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the logout page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_logout_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getLogoutTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getLogoutRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the upload page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @param $query the query string (may be null)
    * @return HTML code
    */ 
   public function get_upload_link($link_text=null, $attrs=null, $query=null)
   {
      if (isset($query))
      {
         $query = "?$query";
      }
      else
      {
         $query = '';
      }

      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getUploadTitle();
      }

      return sprintf("<a href=\"%s%s\"%s>%s</a>",
        $this->getUploadRef(), $query, process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the fallback upload page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @param $query the query string (may be null)
    * @return HTML code
    */ 
   public function get_upload_fallback_link($link_text=null, $attrs=null, $query=null)
   {
      if (isset($query))
      {
         $query = "?$query";
      }
      else
      {
         $query = '';
      }

      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getUploadTitle();
      }

      return sprintf("<a href=\"%s%s\"%s>%s</a>",
        $this->getUploadFallbackRef(), $query, process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the upload lists page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @param $query the query string (may be null)
    * @return HTML code
    */ 
   public function get_upload_lists_link($link_text=null, $attrs=null, $query=null)
   {
      if (isset($query))
      {
         $query = "?$query";
      }
      else
      {
         $query = '';
      }

      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getUploadListsTitle();
      }

      return sprintf("<a href=\"%s%s\"%s>%s</a>",
        $this->getUploadListsRef(), $query, process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the final upload lists page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @param $query the query string (may be null)
    * @return HTML code
    */ 
   public function get_final_upload_lists_link($link_text=null, $attrs=null, $query=null)
   {
      if (isset($query))
      {
         $query = "?$query";
      }
      else
      {
         $query = '';
      }

      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getFinalUploadListsTitle();
      }

      return sprintf("<a href=\"%s%s\"%s>%s</a>",
        $this->getFinalUploadListsRef(), $query, process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the download page.
    * @param $id the download ID
    * @param $type the download type (PDF or log)
    * @param $uid the user ID (may be null)
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_download_link($id, $type, $uid=null, $link_text=null, $attrs=null)
   {
      if (!is_numeric($id) || !($type=='log' || $type=='pdf'))
      {
         return "";
      }

      if (!isset($link_text))
      {
         $link_text = "[$type]";
      }

      $query = "id=$id&amp;type=$type";

      if (isset($uid) && is_numeric($uid))
      {
         $query .= "&amp;uid=$uid";
      }

      return sprintf("<a href=\"%s?%s\"%s>%s</a>",
       $this->getDownloadRef(), $query, process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the FAQ page.
    * @param $link_text the link text (null for default)
    * @param $fragment the fragment parameters (may be null)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_faq_link($link_text=null, $fragment=null, $attrs=null)
   {
      if (!empty($fragment))
      {
         $fragment = "#" . $fragment;
      }
      else
      {
         $fragment = '';
      }

      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getFaqTitle();
      }

      return sprintf("<a href=\"%s%s\"%s>%s</a>",
        $this->getFaqRef(), $fragment, process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the admin page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_admin_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getAdminTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getAdminRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the admin configuration page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_admin_config_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getAdminConfigTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getAdminConfigRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the admin upload directories page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_admin_upload_dirs_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getAdminUploadDirsTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getAdminUploadDirsRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the admin backend maintenance page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_admin_maintenance_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getAdminMaintenanceTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getAdminMaintenanceRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the admin list of users page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @param $query query string (may be null)
    * @return HTML code
    */ 
   public function get_admin_users_link($link_text=null, $attrs=null, $query=null)
   {
      $href = $this->getAdminUsersRef();

      if (!empty($query))
      {
         $href .= "?$query";
      }

      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getAdminUsersTitle();
      }

      return "<a href=\"$href\"" . process_attributes($attrs) . ">$link_text</a>";
   }

   /**
    * Gets HTML code to link to the admin PHP information page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_admin_phpinfo_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getAdminPhpInfoTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getAdminPhpInfoRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the admin view process logs page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_admin_viewlogs_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getAdminViewLogsTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getAdminViewLogsRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the admin who's online page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_admin_whosonline_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getAdminWhosOnlineTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getAdminWhosOnlineRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the admin session data page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_admin_sessions_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getAdminSessionsTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getAdminSessionsRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets HTML code to link to the admin action recorder page.
    * @param $link_text the link text (null for default)
    * @param $attrs attributes to add to the 'a' element (may be
    * null)
    * @return HTML code
    */ 
   public function get_admin_view_action_recorder_link($link_text=null, $attrs=null)
   {
      if (!isset($link_text))
      {
         $link_text = $this->passConfig->getAdminViewActionRecorderTitle();
      }

      return sprintf("<a href=\"%s\"%s>%s</a>",
        $this->getAdminViewActionRecorderRef(), process_attributes($attrs), $link_text);
   }

   /**
    * Gets the configuration settings array.
    * @return array of configuration settings
    */ 
   public function getConfigSettings()
   {
      return array_keys($this->config);
   }

   /**
    * Gets the given configuration value.
    * @param $key configuration key
    * @return value or null if not set
    */ 
   public function getConfigValue($key)
   {
      return isset($this->config[$key]) && isset($this->config[$key]['value']) ? $this->config[$key]['value'] : null;
   }

   /**
    * Gets the given configuration description.
    * @param $key configuration key
    * @return value or false if not set
    */ 
   public function getConfigDescription($key)
   {
      if (!(isset($this->config[$key]) && isset($this->config[$key]['description'])))
      {
         return false;
      }

      if ($this->config[$key]['value_type'] === 'text')
      {
         return $this->config[$key]['description']
            . ' Allowed tags: <code>'
            . htmlentities(preg_replace('/></', '> <', self::CONFIG_TEXT_ALLOWED_TAGS))
            . '</code>.';
      }
      else if ($this->config[$key]['value_type'] === 'match')
      {
         return $this->config[$key]['description']
	    . ' Value must match regex: <code>'
	    . htmlentities($this->config[$key]['value_constraints'])
	    . '</code>.';
      }
      else
      {
         return $this->config[$key]['description'];
      }
   }

   /**
    * Updates the configuration settings.
    * @param $settings the new settings, if null use the page
    * parameters
    */ 
   public function updateConfigSettings($settings=null)
   {
      if (!isset($settings))
      {
         $settings = $this->params;
      }

      $success = true;

      $stmt = $this->db_prepare("UPDATE config SET value=? WHERE id=?");

      if ($stmt === false)
      {
         $this->add_error_message("Failed to update settings");
	 $error_log("Prepared statement failed: " . $this->passdb->error);
	 return false;
      }

      $stmt->bind_param("si", $setting_value, $setting_id);

      foreach ($settings as $key => $setting_value)
      {
         if (isset($this->config[$key]['id']))
	 {
            $setting_id = (int)$this->config[$key]['id'];

	    if ($stmt->execute() === false)
            {
               $this->add_error_message("Failed to update setting '" . htmlentities($key) . "' ID $setting_id");
	       error_log("Prepared statement failed: " . $stmt->error);
               $success = false;
            }
         }
      }

      $stmt->close();

      if ($success)
      {
         foreach ($this->config as $key => $arrayval)
	 {
            if (isset($settings[$key]))
	    {
               $this->config[$key]['value'] = $settings[$key];
	    }
	 }
      }

      return $success;
   }

   /**
    * Connects to the database (if not already connected).
    * This doesn't need calling by any of the frontend pages, as
    * it's automatically called by init_session, but the backend
    * needs to use it in the callback in case the connection has timed-out.
    * @return mysqli connection
    */ 
   public function db_connect()
   {
      if (isset($this->passdb) && is_resource($this->passdb))
      {
         return $this->passdb;
      }

      $this->passdb = passdb_connect();

      // load config

      $result = $this->db_query("SELECT * FROM config");

      if ($result)
      {
         while ($row = mysqli_fetch_assoc($result))
	 {
            $this->config[$row['setting']] = array(
                'id'=>(int)$row['id'],
                'value_type' => $row['value_type'],
                'value_constraints' => $row['value_constraints'],
                'description' => $row['description']
            );

            if ($row['value_type'] === 'int')
	    {
	       $this->config[$row['setting']]['value'] = (int)$row['value'];
	    }
	    else
	    {
	       $this->config[$row['setting']]['value'] = $row['value'];
	    }

	 }
      }

      return $this->passdb;
   }

   /**
    * Closes the database connection.
    */ 
   public function db_disconnect()
   {
      if (isset($this->passdb))
      {
         mysqli_close($this->passdb);
         unset($this->passdb);
      }
   }

   /**
    * Gets the value generated for an AUTO_INCREMENT column by the
    * last query.
    * @return the value or 0 if no previous query or previous query
    * didn't update an AUTO_INCREMENT value or false if no database
    * connection
    */ 
   public function db_insert_id()
   {
      if (!isset($this->passdb))
      {
         error_log("(db_insert_id) no connection to database");
	 return false;
      }

      return mysqli_insert_id($this->passdb);
   }

   /**
    * Gets the number of affected rows in the last MySQL operation.
    * @return the number of affected rows (greater than 0) or 0
    * for no affected rows or -1 if an error occurred on the last
    * query, or false if no database connection
    */ 
   public function db_affected_rows()
   {
      if (!isset($this->passdb))
      {
         error_log("(db_affected_rows) no connection to database");
	 return false;
      }

      return mysqli_affected_rows($this->passdb);
   }

   /**
    * Escapes special characters in the given string, taking into
    * account the current charset of the connection.
    * Usually prepared statements are used instead of this function,
    * but this is used in contexts where there typically wouldn't be
    * any awkward characters anyway (that is, it's just used as an extra safety
    * precaution). For example, usernames or registration numbers
    * that have already been validated.
    *
    * @return the escaped string or false if no connection
    */ 
   public function db_escape_sql($text)
   {
      if (!isset($this->passdb))
      {
         error_log("(db_escape_sql) no connection to database");
	 return false;
      }

      return mysqli_real_escape_string($this->passdb, $text);
   }

   /**
    * Escapes special characters in the given parameter, taking into
    * account the current charset of the connection.
    * @param $paramname the parameter name
    * @return the escaped string or false if no connection
    */ 
   public function db_escape_sql_param($paramname)
   {
      if (!isset($this->passdb))
      {
         error_log("(db_escape_sql_param) no connection to database");
	 return false;
      }

      return mysqli_real_escape_string($this->passdb, $this->params[$paramname]);
   }

   /**
    * Performs a query on the database. Ensure that the query has
    * been properly formatted and strings escaped. Uses MYSQLI_STORE_RESULT
    * result mode.
    * @param $query the query
    * @return a mysqli_result object with buffered result set or
    * false if no connection or failure
    */ 
   public function db_query($query)
   {
      if (!isset($this->passdb))
      {
         error_log("(db_query) no connection to database. Query: $query");

	 return false;
      }

      $result = mysqli_query($this->passdb, $query);

      if (!$result)
      {
         $this->db_error("Query failed: $query");
      }

      return $result;
   }

   /**
    * Performs multiple queries on the database.
    * Use a prepared statement instead where possible. However, this
    * is used in upload_dirs.php to delete multiple submissions.
    * @param $query the query
    * @return false if no connection or if the first statement
    * failed
    */
   public function db_multi_query($query)
   {
      if (!isset($this->passdb))
      {
         error_log("(db_multi_query) no connection to database. Query: $query");
	 return false;
      }

      $result = mysqli_multi_query($this->passdb, $query);

      if (!$result)
      {
         $this->db_error("Query failed: $query");
      }

      return $result;
   }

   /**
    * Prepares a statement. 
    * @param $query the query
    * @return mysqli_stmt or false if failure or no connection
    */ 
   public function db_prepare($query)
   {
      if (!isset($this->passdb))
      {
         error_log("(db_prepare) no connection to database. Query: $query");

	 return false;
      }

      $stmt = $this->passdb->prepare($query);

      if ($stmt === false)
      {
         $this->db_error("failed to prepare statement");
      }

      return $stmt;
   }

   /**
    * Logs an error.
    * @param $msg the error message
    */ 
   public function log_error($msg)
   {
      $err = error_get_last();

      if (isset($err))
      {
         error_log("$msg. Trigger by: " . $err['type'] . ': ' . $err['file'] . ':'
	     . $err['line'] . ': ' . $err['message']);

         error_clear_last();
      }
      else
      {
         $e = new Exception($msg);
         error_log($e);
      }
   }

   /**
    * Logs a database error.
    * @param $msg the error message (if null just use mysqli error
    * message)
    */ 
   public function db_error($msg=null)
   {
      if (!isset($this->passdb))
      {
         $this->log_error("no connection to database");

         if (isset($msg))
         {
            error_log($msg);
         }

	 return false;
      }

      if (isset($msg))
      {
         $this->log_error($msg . '. Error: ' . mysqli_error($this->passdb));
      }
      else
      {
         $this->log_error(mysqli_error($this->passdb));
      }
   }

   /**
    * Adds a generic message to the list of HTML error messages. 
    * Used when some internal error has occurred and the specific
    * details don't need to be told to the user.
    */ 
   public function add_generic_error()
   {
      $this->add_error_message($this->passConfig->getGenericErrorMessage());
   }

   /**
    * Publishes a job to the queue.
    * @param $userinfo array of user information
    * @param $course the course label
    * @param $assignment the assignment label
    * @param $timestamp the submission timestamp
    * @param $token the submission token
    * @param $upload_user_id the UID of the uploader (if null,
    * currently logged in user)
    * @return submission ID if successful, false otherwise
    */ 
   public function publishUpload($userinfo, $course, $assignment, $timestamp, $token,
      $upload_user_id=null)
   {
      $submission_id = false;

      if (isset($upload_user_id))
      {
         if (!$this->isUserRole(self::ROLE_ADMIN))
	 {
            $this->add_error_message('(publishUpload): only admin can publish upload with uploader ID set.');
	    return false;
	 }
      }
      else
      {
         $upload_user_id = $this->user['id'];
      }

      if ($this->db_query(sprintf("INSERT INTO submissions SET uploaded_by=%d, upload_time='%s', course='%s', assignment='%s', token='%s'",
	      $upload_user_id,
	      $this->db_escape_sql($timestamp), 
	      $this->db_escape_sql($course),
	      $this->db_escape_sql($assignment),
	      $this->db_escape_sql($token)
      )))
      {
         $submission_id = $this->db_insert_id();

	 if (isset($userinfo['id']) && is_numeric($userinfo['id']))
	 {
	    if (!$this->db_query(sprintf("INSERT INTO projectgroup SET submission_id=%d, user_id=%d",
	          $submission_id, $userinfo['id']
		 )))
	    {
               $this->add_error_message('Failed to insert into projectgroup table.');
	    }
	 }
	 elseif (is_array($userinfo))
	 {
            $success = true;
            $stmt = $this->db_prepare("INSERT INTO projectgroup (submission_id, user_id) VALUES ($submission_id, ?)");

            if ($stmt === false)
            {
	       $success = false;
            }
            else
	    {
	       $stmt->bind_param("i", $user_id);

	       foreach ($userinfo as $info)
	       {
                  if (!is_array($info) || !isset($info['id']) || !is_numeric($info['id']))
		  {
                     $this->log_error("Prepared statement failed. No int 'id' in ".var_export($info, true));
		  }
		  else
		  {
                     $user_id = (int)$info['id'];

	             if ($stmt->execute() === false)
                     {
	                $this->db_error("Prepared statement failed");
                        $success = false;
                     }
                  }
	       }

	       $stmt->close();
	    }

	    if (!$success)
            {
               $this->add_error_message('Failed to insert items into projectgroup table.');
	    }
	 }
	 else
	 {
	    if (!$this->db_query(sprintf("INSERT INTO projectgroup SET submission_id=%d, user_id=%d",
	          $submission_id, $this->user['id']
		 )))
	    {
               $this->add_error_message('Failed to insert into projectgroup table.');
	    }
	 }
      }
      else
      {
         $this->add_error_message('Failed to insert submission data into table.');
      }

      try
      {
         $connection = connectProducer();
         $channel = $connection->channel();

         declareQueue($channel);

	 $msg = json_encode(array('user'=>$this->getUserName(),
	   'assignment'=>$assignment,
           'time'=>$timestamp, 'token'=>$token, 
	   'submission_id'=>$submission_id,
	   'messages'=>($this->isDebugCourseEnabled() ? 'debug' : 'silent')));

         if ($msg === false)
         {
            $result = false;
	    $this->log_error('Failed to encode JSON data: ' . json_last_error_msg());
            $this->add_error_message('Failed to encode JSON data.');
         }
         else
         {
            publishMessage($channel, $msg);
	    $this->updateSubmissionStatus($submission_id, 'queued');
         }

         $channel->close();
         $connection->close();
      }
      catch (Exception $e)
      {
         error_log($e->getMessage());

         $this->add_error_message($this->passConfig->getQueueOfflineText($submission_id));
      }

      return $submission_id;
   }

   /**
    * Requeues a job (admin only).
    * @param $data the job data
    * @return true if successful, false otherwise
    */ 
   public function requeueUpload($data)
   {
      if (!$this->isUserRole(self::ROLE_ADMIN))
      {
         $this->add_error_message('requeueUpload requires admin role');
	 return false;
      }

      if (!isset($data['user']))
      {
         $this->add_error_message('(requeueUpload) Missing \'user\'');
	 return false;
      }

      if (!isset($data['assignment']))
      {
         $this->add_error_message('(requeueUpload) Missing \'assignment\'');
	 return false;
      }

      if (!isset($data['time']))
      {
         $this->add_error_message('(requeueUpload) Missing \'time\'');
	 return false;
      }

      if (!isset($data['token']))
      {
         $this->add_error_message('(requeueUpload) Missing \'token\'');
	 return false;
      }

      if (!isset($data['submission_id']))
      {
         $this->add_error_message('(requeueUpload) Missing \'submission_id\'');
	 return false;
      }

      if (!isset($data['messages']))
      {
         $data['messages'] = ($this->isDebugCourseEnabled() ? 'debug' : 'silent');
      }

      try
      {
         $result = true;
         $connection = connectProducer();
         $channel = $connection->channel();

         declareQueue($channel);

	 $msg = json_encode($data);

         if ($msg === false)
         {
            $result = false;
	    $this->log_error('Failed to encode JSON data: ' . json_last_error_msg());
            $this->add_error_message('Failed to encode JSON data.');
         }
         else
         {
            publishMessage($channel, $msg);
         }

         $channel->close();
         $connection->close();

	 return $result;
      }
      catch (Exception $e)
      {
         error_log($e->getMessage());
         $this->add_error_message('Failed to publish message. Check if RabbitMQ is still running.');
      }

      return false;
   }

   /**
    * Sends an email. Only allows for one "To" recipient and a
    * maximum of one "CC" and one "BCC". (For group projects, send a
    * separate email per user.)
    * @param $subject the subject (this will have a prefix added)
    * @param $message the message (this will have line wrapping,
    * prefix and suffix added)
    * @param $toaddress username or email address or null for
    * currently logged in user
    * @param $ccaddress CC username or email address or null if none
    * @param $bccaddress BCC username or email address or null if none
    * @return true if successful, false otherwise
    */ 
   public function send_email($subject, $message, $toaddress=null, $ccaddress=null, $bccaddress=null)
   {
      if (!isset($toaddress))
      {
         $toaddress = $this->getUserEmail();
      }
      elseif (!preg_match('/@/', $toaddress))
      {
         $toaddress = $this->getEmailAddress($toaddress);
      }

      $toaddress = $this->processEmailAddress($toaddress);

      if (!$toaddress || !filter_var($toaddress, FILTER_VALIDATE_EMAIL))
      {
	 $this->add_error_message('Invalid to address');
	 $this->log_error("invalid to address: $toaddress");
         return false;
      }

      $headers = array(
        'From' => $this->config['mail_from']['value'],
	'Content-Type' => 'text/plain; charset=UTF-8'
      );

      if (isset($ccaddress))
      {
         $ccaddress = $this->processEmailAddress($ccaddress);

         if (!filter_var($ccaddress, FILTER_VALIDATE_EMAIL))
	 {
	    $this->add_error_message('Invalid cc address');
	    $this->log_error("invalid cc address: $ccaddress");
	    return false;
	 }

	 $headers['Cc'] = $ccaddress;
      }

      if (isset($bccaddress))
      {
         $bccaddress = $this->processEmailAddress($bccaddress);

         if (!filter_var($bccaddress, FILTER_VALIDATE_EMAIL))
	 {
	    $this->add_error_message('Invalid bcc address');
	    $this->log_error("invalid bcc address: $bccaddress");
	    return false;
	 }

	 $headers['Bcc'] = $bccaddress;
      }

      $subject = str_replace(array("\r", "\n"), '', trim($subject));

      $sendmail_additional_args = "-f " . $this->config['envelope_from']['value'];

      if ($this->isDebugMode(2))
      {
         error_log("[DEBUG(2)] sending mail to $toaddress, subject: $subject, headers: " . var_export($headers, true) . "; additional sendmail arguments: $sendmail_additional_args" . "; message: $message");
      }

      $result = mail($toaddress, $this->passConfig->getMailSubject($subject),  
	     $this->passConfig->getMailMessage($message), $headers, 
         $sendmail_additional_args);

      if ($this->isDebugMode(2))
      {
         error_log("[DEBUG(2)] mail returned: " . var_export($result, true));
      }

      if (!$result)
      {
         $this->log_error("failed to send email\n\tTo: $toaddress\n\tSubject: $subject\n");
	 $this->add_error_message('An error occurred when sending email.');
      }

      return $result;
   }

   /**
    * Sends an email notification that a job has been processed.
    * @param $exitCode the job exit code
    * @param $job array of job data
    * @return an array of error messages or an empty array if no errors occurred
    */ 
   public function sendJobProcessedEmail(int $exitCode, array $job)
   {
      return $this->passConfig->sendJobProcessedEmail($exitCode, $job);
   }

   /**
    * Gets a parameter. The parameter array is used to store script
    * parameters that have been validated. Unvalidated $_GET or
    * $_POST variables shouldn't be added to the parameter array.
    * If a parameter fails its validation check, the parameter isn't
    * set, but an associated error parameter is set instead with the
    * error tag as its value. The parameter array can also be used
    * to store additional information required by the script.
    * @param $name the parameter name
    * @return the parameter value or null if not set
    */ 
   public function getParam($name)
   {
      return isset($this->params[$name]) ? $this->params[$name] : null;
   }

   /**
    * Checks if an error has been raised on the given parameter.
    * @param $name the parameter name
    * @return true if error has occurred for the given parameter,
    * false otherwise
    */ 
   public function isErrorParamSet($name)
   {
      return isset($this->params[$name.'error']) ? true : false;
   }

   /**
    * Gets the error tag for the given parameter.
    * @param $name the parameter name
    * @return the error tag
    */ 
   public function getErrorParam($name)
   {
      return $this->params[$name.'error'];
   }

   /**
    * Gets a parameter element.
    * For use where the parameter value is an array.
    * @param $name the parameter name
    * @param $key the element key
    * @return the parameter value or null if not set
    */ 
   public function getParamElement($name, $key)
   {
      return $this->params[$name][$key];
   }

   /**
    * Tests if the given parameter is equal to the given value.
    * @param $name the parameter name
    * @param $value the value to be tested
    * @param $exact if true use === otherwise use ==
    * @return true if the given parameter is set and is equal to the
    * given value or false otherwise
    */ 
   public function isParam($name, $value, $exact=true)
   {
      if (isset($this->params[$name]) &&
         (($exact && $this->params[$name] === $value) 
          || (!$exact && $this->params[$name] == $value)))
      {
         return true;
      }
      else
      {
         return false;
      }
   }

   /**
    * Tests if the given parameter is set and not an empty string.
    * @param $name the parameter name
    * @return true if the given parameter has been set and is not
    * an empty string, false otherwise
    */ 
   public function isParamSet($name)
   {
      return (isset($this->params[$name]) && $this->params[$name] !== '') ? true : false;
   }

   /**
    * Tests if the given parameter element is set and not an empty string.
    * For use where the parameter value is an array.
    * @param $name the parameter name
    * @param $key the element key
    * @return true if the given parameter element has been set and is not
    * an empty string, false otherwise
    */ 
   public function isParamElementSet($name, $key)
   {
      return (isset($this->params[$name]) && isset($this->params[$name][$key])
	      && $this->params[$name][$key] !== '') ? true : false;
   }

   /**
    * Tests if the given parameter element is equal to the given value.
    * For use where the parameter value is an array.
    * @param $name the parameter name
    * @param $key the element key
    * @param $value the value to be tested
    * @param $exact if true use === otherwise use ==
    * @return true if the given parameter element is set and is equal to the
    * given value or false otherwise
    */ 
   public function isParamElement($name, $key, $value, $exact=true)
   {
      if (isset($this->params[$name]) && isset($this->params[$name][$key]) &&
         (($exact && $this->params[$name][$key] === $value) 
          || (!$exact && $this->params[$name][$key] == $value)))
      {
         return true;
      }
      else
      {
         return false;
      }
   }

   /**
    * Tests if the given parameter is an array.
    * @param $name the parameter name
    * @return if the given parameter value is an array, false
    * otherwise
    */ 
   public function isParamArray($name)
   {
      return (isset($this->params[$name]) && is_array($this->params[$name])) ? true : false;
   }

   /**
    * Tests if a boolean parameter is on.
    * Note that an unset or empty parameter is considered false.
    * @param $name the parameter name
    * @return true if parameter value isn't false
    */ 
   public function isBoolParamOn($name)
   {
      return empty($this->params[$name]) ? false : true;
   }

   /**
    * Sets a boolean parameter.
    * @param $name the parameter name
    * @param $state true if parameter should be on, false otherwise
    */ 
   public function setBoolParam($name, $state)
   {
      $this->params[$name] = ($state ? 'on' : false);
   }

   /**
    * Sets the given parameter to the given value.
    * @param $name the parameter name
    * @param $value the new value
    */ 
   public function setParam($name, $value)
   {
      $this->params[$name] = $value;
   }

   /**
    * Unsets a parameter.
    * @param $name the parameter name
    */ 
   public function unsetParam($name)
   {
      unset($this->params[$name]);
   }

   /**
    * Sets the parameter element to the given value.
    * For use where the parameter value is an array.
    * @param $name the parameter name
    * @param $key the element key
    * @param $value the new value
    */ 
   public function setParamElement($name, $key, $value)
   {
      $this->params[$name][$key] = $value;
   }

   /**
    * Tests if courses with the 'debug' attribute set to true should
    * be made available in the upload interface.
    * @return true if debugging courses should be available, false
    * otherwise
    */ 
   public function isDebugCourseEnabled()
   {
      if ($this->config['debug']['value'] === -1)
      {
         if ($this->isUserStaffOrAdmin()
          || ($this->getConfigValue('allow_test_user')===1 && $this->isTestUser())
            )
         {
            return true;
         }
         else
         {
            return false;
         }
      }

      return $this->isDebugMode();
   }

   /**
    * Tests if the site is in debug mode. Debugging is consider on
    * if the debug level is greater than 0. Level 0 means no
    * debugging. Level -1 means no debugging messages but allows debugging
    * course for staff or admin or the test user.
    * @param $level the debug level
    * @param $exact if true, test for exact match
    * @return true if exact match and debug mode is exactly the
    * given level or if not exact match and debug mode is greater
    * than or equal to the given level, or false otherwise
    */ 
   public function isDebugMode(int $level=1, $exact=false)
   {
      if ($exact)
      {
         return $this->config['debug']['value'] === $level;
      }

      return $this->config['debug']['value'] >= $level;
   }

   /**
    * Writes the given message if debugging mode is at least the
    * given level. Make sure that any HTML special characters are
    * escaped in the message first if used by a web script. 
    * @param $msg the message or an array of messages to write
    * @param $userhtml if true, use HTML markup for line breaks (for
    * web scripts), otherwise use newline character (for backend CLI scripts)
    * @param $level the minimum debugging level
    */ 
   public function debug_message($msg, $usehtml=true, $level=1)
   {
      if ($this->isDebugMode($level))
      {
         if (is_array($msg))
         {
            if ($usehtml)
            {
               echo "<p>[DEBUG ($level)] ", implode('<br>', $msg), "<p>";
            }
            else
            {
               echo "[DEBUG ($level)] ", implode("\n", $msg), "\n";
            }
         }
         else
         {
            if ($usehtml)
            {
               echo "<p>[DEBUG ($level)] $msg<p>";
            }
            else
            {
               echo "[DEBUG ($level)] $msg\n";
            }
         }
      }
   }

   /**
    * Loads the course data from the XML resource file, if it hasn't
    * already been loaded. The returned array has the course label as
    * the key and the value is an array with the keys 'name' (course
    * label), 'href' (URL to the assignment XML file), and 'title'
    * (the course title). The course data is stored in a private
    * variable and also in the session data to save repeatedly
    * parsing the XML files.
    * @return the course data array
    */ 
   public function load_courses()
   {
      if (isset($this->coursedata))
      {
         return $this->coursedata;
      }

      $this->coursedata = array();

      $resources = simplexml_load_file(PassConfig::RESOURCE_PATH);

      $debugCourseEnabled = $this->isDebugCourseEnabled();

      // load locally defined courses
      foreach ($resources->resource as $resource)
      {
         if (empty($resource['debug']) || $debugCourseEnabled 
	      || $resource['debug'] === 'off' || $resource['debug'] === 'false')
	 {
	    if (!isset($resource['name']))
	    {
	       error_log("Missing resource name in '" . PassConfig::RESOURCE_PATH
		    . "' " . var_export($resource, true));
	    }
	    elseif (!isset($resource['href']))
	    {
	       error_log("Missing resource href in '" . PassConfig::RESOURCE_PATH
		    . "' " . var_export($resource, true));
	    }
	    else
	    {
	       $name = (string)$resource['name'];

	       $this->coursedata[$name] = 
	         array('name'=>$name, 
	              'href'=>(string)$resource['href'],
		      'title'=>(string)$resource
	       );
	    }
	 }
      }

      if (isset($resources->courses))
      {
         $url = (string)$resources->courses['href'];
         $headers = get_headers($url);

	 if (!preg_match('/200 OK/', $headers[0]))
	 {
            $external_resources = false;
	 }
	 else
	 {
            $external_resources = simplexml_load_file($url);
	 }

	 if ($external_resources === false)
	 {
            foreach ($resources->courses->resource as $resource)
            {
               if ($debugCourseEnabled || empty($resource['debug'])
		    || $resource['debug'] === 'off' || $resource['debug'] === 'false')
	       {
	          if (!isset($resource['name']))
	          {
	             error_log("Missing resource name in '" . PassConfig::RESOURCE_PATH
		          . "' " . var_export($resource, true));
	          }
	          elseif (!isset($resource['href']))
	          {
	             error_log("Missing resource href in '" . PassConfig::RESOURCE_PATH
		          . "' " . var_export($resource, true));
	          }
	          else
	          {
	             $name = (string)$resource['name'];
      
	             $this->coursedata[$name] = 
	               array('name'=>$name, 
	                     'href'=>(string)$resource['href'],
		             'title'=>(string)$resource
	             );
	          }
	       }
            }
	 }
	 else
	 {
            foreach ($external_resources->resource as $resource)
            {
               if ($debugCourseEnabled || empty($resource['debug'])
		    || $resource['debug'] === 'off' || $resource['debug'] === 'false')
	       {
	          if (!isset($resource['name']))
	          {
	             error_log("Missing resource name in '$url' " . var_export($resource, true));
	          }
	          elseif (!isset($resource['href']))
	          {
	             error_log("Missing resource href in '$url' " . var_export($resource, true));
	          }
	          else
	          {
	             $name = (string)$resource['name'];
      
	             $this->coursedata[$name] = 
	               array('name'=>$name, 
	                     'href'=>(string)$resource['href'],
		             'title'=>(string)$resource
	             );
	          }
	       }
            }
	 }
      }

      $_SESSION['coursedata'] = $this->coursedata;

      return $this->coursedata;
   }

   /**
    * Gets an array of all available courses. This will load the course data,
    * if it hasn't already been loaded.
    * @return the course data or false if unavailable
    */ 
   public function fetch_courses()
   {
      $this->load_courses();

      return isset($this->coursedata) ? $this->coursedata : false;
   }

   /**
    * Gets the data for the given course. All course data will be
    * loaded, if not already available.
    * @param $courselabel the course label
    * @return the array for the given course or false if not found
    */ 
   public function fetch_course($courselabel)
   {
      $this->load_courses();

      return isset($this->coursedata) && isset($this->coursedata[$courselabel]) ?
	      $this->coursedata[$courselabel] : false;
   }

   /**
    * Gets the URL for the assignment XML data associated with the given
    * course. All course data will be loaded, if not already available.
    * @param $courselabel the course label
    * @return the 'href' attribute value for the given course or
    * false if not found
    */ 
   public function fetch_courseurl($courselabel)
   {
      $this->load_courses();

      $course = $this->fetch_course($courselabel);

      if ($course === false)
      {
         error_log("Course '$courselabel' not found.");
	 return false;
      }

      return isset($course['href']) ? $course['href'] : false;
   }

   /**
    * Compares two assignments. This is used to order available
    * assignments according to their due date, so that the course
    * closest to its due date can be found first. Since the due date
    * is in ISO format, a simple string comparison can be used.
    *
    * @param $a array representing one assignment
    * @param $b array representing another assignment
    * @return result of string comparison on the due date
    */ 
   private function cmp_assignments($a, $b)
   {
      if ($a === $b)
      {
         return 0;
      }

      return strcmp($b['due'], $a['due']);
   }

   /**
    * Loads all assignments for the given course. If the course URL
    * hasn't been specified, it can be obtained from the course
    * data (which will have to be loaded). If the assignment
    * information has already been loaded, it will simply be
    * returned, otherwise it will be loaded, and the assignment data
    * will be stored in the 'assignmentdata' session variable.
    * @param $courselabel the course label
    * @param $courseurl the URL of the assignment XML file or null
    * if the information should be obtained from the course data
    * @return the array of assignments for the given course or false
    * if not found
    */ 
   public function load_assignments($courselabel, $courseurl=null)
   {
      if (isset($this->assignmentdata) && isset($this->assignmentdata[$courselabel]))
      {
         return $this->assignmentdata[$courselabel];
      }

      if (!isset($this->assignmentdata))
      {
         $this->assignmentdata = array();
      }

      $this->assignmentdata[$courselabel] = array();

      if (!isset($courseurl))
      {
         $this->load_courses();

         if (!isset($this->coursedata[$courselabel]))
         {
            if ($this->isDebugMode(1))
            {// maybe an old course that's been removed or renamed
               error_log("Unable to fetch data for course '$courselabel'");
            }

            return false;
         }

	 $courseurl = $this->coursedata[$courselabel]['href'];
      }

      $assignments = simplexml_load_file($courseurl);

      if ($assignments === false)
      {
         if ($this->isDebugMode(1))
         {// maybe an old course that's been removed or renamed or URL temporarily unavailable
            error_log("Unable to fetch assignments from '$courseurl' for course '$courselabel'");
         }

         return false;
      }
      else
      {
         foreach ($assignments->assignment as $assignment)
         {
            $name = (string)$assignment['name'];

            if (!empty($name))
            {
	       $this->assignmentdata[$courselabel][$name] = array(
	          'name'=>$name,
	          'due'=>trim((string)$assignment->due),
	          'title'=>trim((string)$assignment->title)
	       );

	       if (isset($assignment['language']))
	       {
                  $this->assignmentdata[$courselabel][$name]['language']
                     = (string)$assignment['language'];
	       }

               if (isset($assignment['relpath']))
               {
                  $this->assignmentdata[$courselabel][$name]['relpath']
                    = preg_match('/^(true|on)$/', trim($assignment['relpath']));
               }
               else
               {
                  $this->assignmentdata[$courselabel][$name]['relpath']=false;
               }

	       if (isset($assignment->mainfile))
	       {
	          $this->assignmentdata[$courselabel][$name]['mainfile']
		       = trim((string)$assignment->mainfile);
	       }

	       if (isset($assignment->file))
	       {
                  $files = array();

                  foreach ($assignment->file as $file)
		  {
	             array_push($files, trim((string)$file));
		  }

		  $this->assignmentdata[$courselabel][$name]['files'] = $files;
	       }

	       if (isset($assignment->resourcefile))
	       {
                  $files = array();

                  foreach ($assignment->resourcefile as $file)
		  {
                     $src = trim((string)$file['src']);
		     $srcpath = parse_url($src, PHP_URL_PATH);
	             array_push($files, basename($srcpath));
		  }

		  $this->assignmentdata[$courselabel][$name]['resourcefiles'] = $files;
	       }

	       if (isset($assignment->resultfile))
	       {
                  $files = array();

                  foreach ($assignment->resultfile as $file)
		  {
	             array_push($files, trim((string)$file['name']));
		  }

		  $this->assignmentdata[$courselabel][$name]['resultfiles'] = $files;
	       }

	       if (isset($assignment->allowedbinary))
	       {
                  $accept_ext = null;
                  $accept_type = null;
                  $num_exts = 0;
                  $accept_ext_array = array();

                  foreach ($assignment->allowedbinary as $filter)
		  {
                     $extensions = explode(',', trim($filter['ext']));

                     foreach ($extensions as $ext)
                     {
                        $ext = trim($ext);

                        if (!empty($ext))
                        {
                           $num_exts++;

                           if (!isset($accept_ext))
                           {
                              $accept_ext = ".$ext";
                           }
                           else
                           {
                              $accept_ext .= ", .$ext";
                           }

                           array_push($accept_ext_array, $ext);
                        }
                     }

                     if (!isset($accept_type))
                     {
                        $accept_type = $filter['type'];
                     }
                     else
                     {
                        $accept_type .= '|' . $filter['type'];
                     }
		  }

                  $accept = array('exts'=>$accept_ext, 'types'=>$accept_type,
                    'num_exts'=>$num_exts, 'ext_array'=>$accept_ext_array);

	          $this->assignmentdata[$courselabel][$name]['allowedbinaries'] = $accept;
	       }

	       if (isset($assignment->report))
	       {
                  $files = array();

                  foreach ($assignment->report as $file)
		  {
	             array_push($files, trim((string)$file));
		  }

		  $this->assignmentdata[$courselabel][$name]['reports'] = $files;
	       }
	    }
	    else
	    {
	       error_log("Missing assignment name in '$courseurl' "
	         . var_export($resource, true));
	    }
         }

	 if (!uasort($this->assignmentdata[$courselabel], array($this, 'cmp_assignments')))
	 {
            error_log("assignment sort failed " . error_get_last()['message']);
	 }
      }

      if (!isset($_SESSION['assignmentdata']))
      {
         $_SESSION['assignmentdata'] = $this->assignmentdata;
      }

      return $this->assignmentdata[$courselabel];
   }

   /**
    * Gets an array of assignments for the given course.
    * This will load the assignment data if it hasn't already been
    * loaded. The returned array will have the assignment labels as
    * keys and the associated assignment array data as values.
    *
    * @param $courselabel the course label
    * @param $courseurl the URL of the assignment data or null if it
    * should be obtained from the course data
    * @return an array of assignment array data or false if not found
    */ 
   public function fetch_assignments($courselabel, $courseurl=null)
   {
      $this->load_assignments($courselabel, $courseurl);

      return isset($this->assignmentdata[$courselabel]) ? $this->assignmentdata[$courselabel] : false;
   }

   /**
    * Gets the data for a specific assignment.
    * This will load the assignment data if it hasn't already been
    * loaded. The returned array will have the 'name' element set to
    * the assignment label, 'due' set to the due date, and 'title'
    * set to the assignment title. The other elements are optional
    * and may not be set: 'language' (the default language label), 'relpath'
    * (boolean value to indicate whether or not relative paths must be used),
    * 'mainfile' (the required main source code filename), 'files' (array of
    * other required files), 'resourcefiles' (array of resource
    * file basenames), 'resultfiles' (array of result filenames), and
    * 'allowedbinaries' (array of allowed binary information).
    *
    * The 'allowedbinaries' array value has the keys: 'exts'
    * (allowed extensions with a leading dot as a comma-separated list), 'types' (file
    * filter accept types), 'num_exts' the number of allowed
    * extensions, and 'ext_array' (the allowed extensions without a
    * leading dot as an array).
    *
    * @param $courselabel the course label
    * @param $assignmentlabel the assignment label
    * @param $courseurl the URL of the assignment data or null if it
    * should be obtained from the course data
    * @return the assignment array data or false if not found
    */ 
   public function fetch_assignment($courselabel, $assignmentname, $courseurl=null)
   {
      $this->load_assignments($courselabel, $courseurl);

      if (!isset($this->assignmentdata) 
	      || !isset($this->assignmentdata[$courselabel])
	      || !isset($this->assignmentdata[$courselabel][$assignmentname]))
      {
         if ($this->isDebugMode(1))
         {// maybe an old assignment that's been removed or renamed
            $this->log_error("Unable to fetch assignment '$assignmentname' for course '$courselabel'. Assignment data: " .  var_export($this->assignmentdata, true));
         }

         return false;
      }

      return $this->assignmentdata[$courselabel][$assignmentname];
   }

   /**
     * Tests if the given password is considered insecure.
     * This checks for common passwords and easy to guess patterns.
     * The minimum length should have already been checked.
     * The createPasswordHash(string,string) function will check if
     * password is the same as the username. (The username may not
     * be known at this point.) 
     * @return true if common or easily to guess pattern, false
     * otherwise
    */
   public function is_insecure_password($password)
   {
      if (preg_match('/^(pa[s\$][s\$]w[o0]rd|qwerty(uiop)?)[\d!\$@\.]*$/i', $password)
       || preg_match('/^(123|321|789|987|abc|qwe|xyz|zyx)+$/i', $password)
       || preg_match('/^0?1234567?8?9?0?$/', $password)
       || preg_match('/^0?9?8?7?6543210?$/', $password)
       || preg_match('/^0+$/', $password)
       || preg_match('/^1+$/', $password)
       || preg_match('/^2+$/', $password)
       || preg_match('/^3+$/', $password)
       || preg_match('/^4+$/', $password)
       || preg_match('/^5+$/', $password)
       || preg_match('/^6+$/', $password)
       || preg_match('/^7+$/', $password)
       || preg_match('/^8+$/', $password)
       || preg_match('/^9+$/', $password)
       || preg_match('/^(01|10|69|12)+$/', $password)
       || preg_match('/^(zaq1?|1?qaz)(2?wsx|xsw2?)$/i', $password)
       || preg_match('/^(ilove(yo)?u|monkey|dragon|leopard|pussy|myn[o0][o0]b|google|asdfghjkl|zxcvbnm|18atcskd2w|1q2w3e(4r(t5(y6)?)?)?|3rjs1la7qe|princess|football|baseball|letmein|login|sunshine|shadow|trustno(one)?|access|master|admin|lovely|hottie|whatever|loveme|starwars|donald|superman|batman|welcome|hello|qazwsx(edc)?|azerty|jesus|michael|ashley|aaron|jordan|jennifer|ninja|flower|mustang|fuck(er|off|(yo)?u)?|killer|picture|senha|million|omgpop|unknown|chatbooks|jacket|bangbang|jobandtalent|default|changeme|biteme|matrix|freedom|a[s\$]{2}h[o0][l1]e|secret)[\d!\$@\.]*$/i', $password))
      {
         return true;
      }

      return false;
   }

   /*
    * The get_xxx_variable functions are all designed to test if
    * the associated script parameter is set and is valid. If so,
    * the value is stored in the private params array. If the value
    * isn't valid (or is missing if required), an error is raised
    * and a tag is set for the corresponding error parameter instead
    * of setting the actual parameter element.
    *
    * A default value can be provided if the corresponding
    * $_POST or $_GET value hasn't been set, but the default won't 
    * be validated, so don't use any unvalidated input for default values.
    *
    * Some of these functions allow arrays as parameter values.
    */ 

   /**
    * Gets a password variable. This checks the $_POST value for the given field.
    * If not set, it raises a 'Missing' error on the field. The value only
    * needs to be checked for minimum length and whether or not it's easily
    * guessable if it's a new password (rather than being supplied
    * for verification purposes).
    *
    * @param $field the name of the script parameter
    * @param $check_criteria true if value should be checked for
    * minimum length or if it's obviously insecure, false for no
    * check
    * @param $name the name of the field to use in any error message
    */
   public function get_password_variable(string $field, $check_criteria=false, $name='password')
   {
      if (!isset($_POST[$field]))
      {
         $this->add_error_message($this->passConfig->getMissingText($name),
            $field, $this->passConfig->getMissingTag());

	 return;
      }

      $plaintext = $_POST[$field];

      if ($check_criteria)
      {
         if (strlen($plaintext) < self::MIN_PASSWORD_LENGTH)
	 {
	    $this->add_error_message(
               $this->passConfig->getMinLengthText($name, self::MIN_PASSWORD_LENGTH),
	       $field, $this->passConfig->getTooShortTag());

	    return;
	 }
	 elseif ($this->is_insecure_password($plaintext))
	 {
            $this->add_error_message($this->passConfig->getGuessablePasswordText(),
              $field, $this->passConfig->getInsecureTag());

	    return;
	 }
      }

      $this->params[$field] = $plaintext;
   }

   /**
    * Gets a subpath variable (or array).
    * @param $name the parameter name
    * @param $defaultvalue the default value if not set
    */ 
   public function get_subpath_variable($name, $defaultvalue=null)
   {
      $this->get_matched_variable($name, self::SUB_PATH_PREG, $defaultvalue);
   }

   /**
    * Gets a username variable (or array).
    * @param $name the parameter name
    * @param $defaultvalue the default value if not set
    */ 
   public function get_username_variable($field, $defaultvalue=null)
   {
      $this->get_matched_variable($field,
         $this->passConfig->getUserNamePreg(), $defaultvalue);
   }

   /**
    * Gets a reset token variable. (For account verification or
    * password reset tokens, which consist of 64 hexits.)
    * @param $name the parameter name
    * @param $defaultvalue the default value if not set
    */ 
   public function get_token_variable($field, $defaultvalue=null)
   {
      $this->get_matched_variable($field, self::TOKEN_PREG, $defaultvalue);
   }

   /**
    * Tests if the given value is a valid registration number.
    * @param $value the value to test
    * @return true if value, false otherwise
    */ 
   public function is_valid_regnum($value)
   {
      if (empty($value))
      {
         return false;
      }

      return preg_match($this->passConfig->getRegNumPreg(), $value) === false ? false : true;
   }

   /**
    * Gets the example registration number to use as a placeholder.
    */ 
   public function getRegNumExample()
   {
      return PassConfig::REG_NUM_EXAMPLE;
   }

   /**
    * Gets a registration number variable. Doesn't permit the
    * placeholder example.
    * @param $name the parameter name
    * @param $defaultvalue the default value if not set
    */ 
   public function get_regnum_variable($field, $defaultvalue=null)
   {
      $this->get_matched_variable($field,
        $this->passConfig->getRegNumPreg(), $defaultvalue);

      // There shouldn't be any reason for this to be an array,
      // since there are no pages that require multiple registration
      // numbers to be supplied, but add check for completeness or
      // in case a future version requires this.

      if ($this->isParamArray($field))
      {
         foreach ($this->params[$field] as $value)
         {
            if ($value === $this->getRegNumExample())
            {
               $this->add_error_message(
                 $this->passConfig->getPlaceholderNotValidText(),
                 $field, $this->passConfig->getInvalidTag()
                );

               unset($this->params[$field]);
            }
         }
      }
      elseif ($this->isParam($field, $this->getRegNumExample()))
      {
         $this->add_error_message(
           $this->passConfig->getPlaceholderNotValidText(),
           $field, $this->passConfig->getInvalidTag()
          );

         $this->unsetParam($field);
      }
   }

   /**
    * Gets a from variable.
    * @param $name the parameter name
    * @param $defaultvalue the default value if not set
    */ 
   public function get_from_variable($field='from', $defaultvalue=null)
   {
      $this->get_matched_variable($field, self::FROM_PREG, $defaultvalue);
   }

   /**
    * Gets a boolean variable. If valid, the parameter value will be
    * set to 'on' for true and boolean false otherwise. Any of the
    * following values from the $_GET or $_POST variable indicates
    * 'on'/true: 'on', 'true', 'selected', 'checked' or '1'.
    * Any of the following indicates false: 'off', 'false', '0', or ''.
    *
    * @param $name the parameter name
    * @param $defaultvalue the default value if not set
    */ 
   public function get_boolean_variable($field, $defaultvalue=null)
   {
      if (isset($_POST[$field]))
      {
         $this->params[$field] = $_POST[$field];
      }
      elseif (isset($_GET[$field]))
      {
         $this->params[$field] = $_GET[$field];
      }
      else
      {
         $this->params[$field] = $defaultvalue;
         return;
      }

      if ($this->params[$field] === 'on' || $this->params[$field] === 'true'
          || $this->params[$field] === '1' || $this->params[$field] === 'selected'
          || $this->params[$field] === 'checked')
      {
         $this->params[$field] = 'on';
      }
      elseif ($this->params[$field] === 'off' || $this->params[$field] === 'false'
          || $this->params[$field] === '0' || $this->params[$field] === '')
      {
         $this->params[$field] = false;
      }
      else
      {
         $this->addInvalidFieldError($field);

         $this->params[$field] = $defaultvalue;
      }
   }

   /**
    * Gets an integer variable (or array of integers).
    * @param $name the parameter name
    * @param $defaultvalue the default value if not set
    */ 
   public function get_int_variable($field, $defaultvalue=null)
   {
      if (isset($_POST[$field]))
      {
         $value = $_POST[$field];
      }
      elseif (isset($_GET[$field]))
      {
         $value = $_GET[$field];
      }
      else
      {
         $value = $defaultvalue;
      }

      if ($value==='0')
      {
         $this->params[$field] = 0;
         return;
      }

      if (!isset($value) || $value === '')
      {
         return;
      }

      if (is_array($value))
      {
         $this->params[$field] = array();

         foreach ($value as $elem)
         {
            if (is_numeric($elem))
            {
               if (strlen($elem) > 10)
               {
                  $this->addInvalidFieldError($field);
               }
               else
               {
                  array_push($this->params[$field], (int)$elem);
               }
            }
            else
            {
               $this->addInvalidFieldError($field, 'integer expected');
            }
         }
      }
      elseif (is_numeric($value))
      {
         if (strlen($value) > 10)
         {
            $this->addInvalidFieldError($field);
         }
         else
         {
            $this->params[$field] = (int)$value;
         }
      }
      else
      {
         $this->addInvalidFieldError($field, 'integer expected');
      }
   }

   /**
    * Gets a variable (string or array of strings) that matches the
    * given regular expression.
    * @param $name the parameter name
    * @param $regex the regular expression
    * @param $defaultvalue the default value if not set
    */ 
   public function get_matched_variable($field, $regex, $defaultvalue=null)
   {
      $value = '';

      if (isset($_POST[$field]))
      {
         $value = $_POST[$field];
      }
      elseif (isset($_GET[$field]))
      {
         $value = $_GET[$field];
      }

      if (!isset($value) || $value === '')
      {
         if (isset($defaultvalue))
         {
            $this->params[$field] = $defaultvalue;
         }
      }
      elseif (is_array($value))
      {
         $this->params[$field] = array();

         foreach ($value as $elem)
         {
            if (preg_match($regex, $elem))
            {
               array_push($this->params[$field], $elem);
            }
            else
            {
               $this->addInvalidFieldError($field, 
                   "regex: '". htmlentities($regex) . "'");
            }
         }
      }
      elseif (preg_match($regex, $value))
      {
         $this->params[$field] = $value;
      }
      else
      {
         $this->addInvalidFieldError($field, 
           "regex: '". htmlentities($regex) . "'");
      }
   }

   /**
    * Gets a datetime variable.
    * The value must be in one of the following formats:
    * Y-m-d\TH:i:s
    * Y-m-d H:i:s
    * Y-m-d\TH:i
    * Y-m-d H:i
    * The parameter value will be an instance of DateTime.
    * This function checks if the default value matches the required
    * format if the supplied value isn't a DateTime instance and
    * isn't null or empty.
    *
    * @param $name the parameter name
    * @param $default the default value if not set
    */
   public function get_datetime_variable($field, $default=null)
   {
      if (isset($_POST[$field]))
      {
         $value = $_POST[$field];
      }
      elseif (isset($_GET[$field]))
      {
         $value = $_GET[$field];
      }
      elseif ($default instanceof DateTime)
      {
         $this->params[$field] = $default;
         return;
      }
      else
      {
         $value = $default;
      }

      if (empty($value))
      {
         return;
      }

      if (preg_match('/^(\d{4}-\d{2}-\d{2})(?:[T ])(\d{2}:\d{2})(\:\d{2})?$/',
            $value, $matches))
      {
         if (empty($matches[3]))
         {// no seconds
            $this->params[$field] = date_create_from_format('Y-m-d H:i',
               $matches[1] . ' ' . $matches[2]);
         }
	 else
	 {
            $this->params[$field] = date_create_from_format('Y-m-d H:i:s',
              $matches[1] . ' ' . $matches[2] . $matches[3]);
	 }
      }
      else
      {
         $this->addInvalidFieldError($field);
      }
   }

   /**
    * Gets a date variable. Similar to get_datetime_variable but
    * the value must be in the form Y-m-d
    * The parameter value will be an instance of DateTime.
    * @param $name the parameter name
    * @param $default the default value if not set
    */
   public function get_date_variable($field, $default=null)
   {
      if (isset($_POST[$field]))
      {
         $value = $_POST[$field];
      }
      elseif (isset($_GET[$field]))
      {
         $value = $_GET[$field];
      }
      elseif ($default instanceof DateTime)
      {
         $this->params[$field] = $default;
         return;
      }
      else
      {
         $value = $default;
      }

      if (empty($value))
      {
         return;
      }

      if (preg_match('/^\d{4}-\d{2}-\d{2}$/', $value))
      {
         $this->params[$field] = date_create_from_format('Y-m-d', $value);
      }
      else
      {
         $this->addInvalidFieldError($field);
      }
   }

   /**
    * Gets a time variable. Similar to get_datetime_variable but
    * the value must be in one of the following formats:
    * H:i:s
    * H:i
    * The parameter value will be an instance of DateTime.
    * @param $name the parameter name
    * @param $default the default value if not set
    */
   public function get_time_variable($field, $default=null)
   {
      if (isset($_POST[$field]))
      {
         $value = $_POST[$field];
      }
      elseif (isset($_GET[$field]))
      {
         $value = $_GET[$field];
      }
      elseif ($default instanceof DateTime)
      {
         $this->params[$field] = $default;
         return;
      }
      else
      {
         $value = $default;
      }

      if (empty($value))
      {
         return;
      }

      if (preg_match('/^(\d{2}:\d{2})(\:\d{2})?$/', $value, $matches))
      {
         if (empty($matches[2]))
         {// no seconds
            $this->params[$field] = date_create_from_format('H:i', $value);
         }
	 else
	 {
            $this->params[$field] = date_create_from_format('H:i:s', $value);
	 }
      }
      else
      {
         $this->addInvalidFieldError($field);
      }
   }

   /**
    * Gets a variable (string or array of strings) that needs to have HTML entities escaped.
    * Note that no escaping is performed on the default value.
    * @param $field the parameter name
    * @param $default the default value if not set
    * @param $flags the htmlentities flag
    */
   public function get_htmlentities_variable($field, $default=null, $flags=ENT_COMPAT)
   {
      if (isset($_POST[$field]))
      {
         $value = $_POST[$field];
      }
      elseif (isset($_GET[$field]))
      {
         $value = $_GET[$field];
      }
      else
      {
         $this->params[$field] = $default;
         return;
      }

      if (is_array($value))
      {
         $this->params[$field] = array();

         foreach ($value as $elem)
         {
            array_push($this->params[$field], htmlentities($elem, $flags));
         }
      }
      else
      {
         $this->params[$field] = htmlentities($value, $flags);
      }
   }

   /**
    * Gets a variable that hasn't been validated. Use with extreme
    * caution! Intended for values that won't be written to STDOUT
    * or that will be escaped before being written but require some
    * preprocessing beforehand.
    *
    * @param $field the parameter name
    * @param $default the default value if not set
    */ 
   function get_unfiltered_variable($field, $defaultvalue=null)
   {
      if (isset($_POST[$field]))
      {
         $this->params[$field] = $_POST[$field];
      }
      elseif (isset($_GET[$field]))
      {
         $this->params[$field] = $_GET[$field];
      }
      elseif (isset($defaultvalue))
      {
         $this->params[$field] = $defaultvalue;
      }
   }

   /**
    * Gets a variable (string or array of strings) that may contain a limited set of HTML tags.
    * This strips invalid tags using strip_tags. If the result has a
    * different length to the original (which means that content has
    * been stripped) an error will be raised on the field and an
    * associated <code>${field}_striptagsdiff</code> field will be set, with
    * markup to show the invalid content.
    *
    * @param $field the parameter name
    * @param $allowedtags the list of allowed tags, empty indicates
    * strip all tags
    * @param $defaultvalue the default value (null or valid HTML)
    * @param $fieldname the field name for error messages (if null,
    * same as the parameter name)
    */ 
   public function get_striptags_variable($field, $allowedtags='',
     $defaultvalue=null, $fieldname=null)
   {
      if (isset($_POST[$field]))
      {
         $value = $_POST[$field];
      }
      elseif (isset($_GET[$field]))
      {
         $value = $_GET[$field];
      }
      else
      {
         $this->params[$field] = $defaultvalue;
         return;
      }

      if (!isset($fieldname))
      {
         $fieldname = $field;
      }
   
      if (is_array($value))
      {
         $this->params[$field] = array();
   
         foreach ($value as $elem)
         {
            $stripped_value = strip_tags($elem, $allowedtags);
   
            if (strlen($elem) === strlen($stripped_value))
            {
               array_push($this->params[$field], $stripped_value);
            }
            else
            {
               $this->add_error_message(
                 ucfirst($fieldname).' contains invalid content',
                 $field, 'Contains invalid content');
   
               $diff = get_value_diff($elem, $stripped_value);

               if (isset($this->params[$field."_striptagsdiff"]))
               {
                  $this->params[$field."_striptagsdiff"] .= "<hr><p>$diff";
               }
               else
               {
                  $this->params[$field."_striptagsdiff"] = $diff;
               }
            }
         }
      }
      else
      {
         $stripped_value = strip_tags($value, $allowedtags);
   
         if (strlen($value) === strlen($stripped_value))
         {
            $this->params[$field] = $stripped_value;
         }
         else
         {
            $this->add_error_message(
              ucfirst($fieldname).' contains invalid content',
              $field, 'Contains invalid content');
   
            $this->params[$field."_striptagsdiff"] = get_value_diff($value, $stripped_value);
         }
      }
   }

   /**
    * Gets a choice variable (or array).
    * @param $field the parameter name
    * @param $validlist array of valid values
    * @param $defaultvalue the default value
    */ 
   public function get_choice_variable($field, $validlist, $defaultvalue=null)
   {
      if (isset($_POST[$field]))
      {
         $value = $_POST[$field];
      }
      elseif (isset($_GET[$field]))
      {
         $value = $_GET[$field];
      }
   
      if (!isset($value) || $value === '')
      {
         $this->params[$field] = $defaultvalue;
         return;
      }

      if ($value === $defaultvalue)
      {
         $this->params[$field] = $value;
         return;
      }
   
      if (is_array($value))
      {
         $this->params[$field] = array();
   
         foreach ($value as $elem)
         {
            $k = array_search($elem, $validlist);
   
            if ($k === false)
            {
               $this->addInvalidFieldError($field);
   
               return;
            }
            else
            {
               array_push($this->params[$field], $elem);
            }
         }
      }
      else
      {
         $k = array_search($value, $validlist);
   
         if ($k === false)
         {
            $this->addInvalidFieldError($field);

            $this->params[$field] = $defaultvalue;
         }
         else
         {
            $this->params[$field] = $value;
         }
      }
   }

   /**
    * Gets a configuration variable.
    * The value must match the constraints for the given field.
    * @param $field the parameter name
    * @param $config_key the name of the corresponding configuration
    * key (if null, same as parameter name)
    */ 
   public function get_config_variable($field, $config_key=null)
   {
      if (!isset($config_key))
      {
         $config_key = $field;
      }

      if (!isset($this->config[$config_key]))
      {
         $this->add_error_message("Unknown config value '".htmlentities($config_key)."'",
            $field, 'Invalid config key');
	 return;
      }

      if (!isset($this->config[$config_key]['value_type']))
      {
         $this->add_error_message("Missing value type for config key '".htmlentities($config_key)."'",
            $field, 'Missing config value type');
	 return;
      }

      if (!isset($this->config[$config_key]['value_constraints']))
      {
         $this->add_error_message("Missing value constraints for config key '".htmlentities($config_key)."'",
            $field, 'Missing config value constraints');
	 return;
      }

      $this->setParam($field, $this->getConfigValue($config_key));

      if ($this->config[$config_key]['value_type'] === 'int')
      {
	 if (preg_match('/^\[([\-+]?\d*),([\-+]?\d*)\]$/', $this->config[$config_key]['value_constraints'], $matches))
	 {
            $min = (empty($matches[1]) ? null : (int)$matches[1]);
            $max = (empty($matches[2]) ? null : (int)$matches[2]);
	 }
	 else
	 {
            $this->add_error_message('Invalid int constraint ' . htmlentities($this->config[$config_key]['value_constraints']), $field, 'Invalid constraint');
	    return;
	 }

         $this->get_int_variable($field, $this->config[$config_key]['value']);

	 if (isset($min) && $this->getParam($field) < $min)
	 {
            $this->add_error_message("Config setting ".htmlentities($config_key) . " must be &gt;= $min");
	    $this->setParam($field, $this->getConfigValue($config_key));
	 }
	 elseif (isset($max) && $this->getParam($field) > $max)
	 {
            $this->add_error_message("Config setting ".htmlentities($config_key) . " must be &lt;= $max");
	    $this->setParam($field, $this->getConfigValue($config_key));
	 }
      }
      elseif ($this->config[$config_key]['value_type'] === 'match')
      {
         $this->get_matched_variable($field, $this->config[$config_key]['value_constraints'], $this->config[$config_key]['value']);
      }
      elseif ($this->config[$config_key]['value_type'] === 'text')
      {
	 if (preg_match('/^\[(\d*),(\d+)\]$/', $this->config[$config_key]['value_constraints'], $matches))
	 {
            $min = (empty($matches[1]) ? 0 : (int)$matches[1]);
            $max = (int)$matches[2];
	 }
	 else
	 {
            $this->add_error_message('Invalid int constraint ' . htmlentities($this->config[$config_key]['value_constraints']), $field, 'Invalid constraint');
	    return;
	 }

         $this->get_striptags_variable($field, self::CONFIG_TEXT_ALLOWED_TAGS, $this->config[$config_key]['value']);

	 $len = strlen($this->getParam($field));

	 if ($len < $min)
	 {
            $this->add_error_message("Config setting ".htmlentities($config_key) . " length must be &gt;= $min");
	    $this->setParam($field, $this->getConfigValue($config_key));
	 }
	 elseif ($len > $max)
	 {
            $this->add_error_message("Config setting ".htmlentities($config_key) . " must be &lt;= $max");
	    $this->setParam($field, $this->getConfigValue($config_key));
	 }
      }
      else
      {
         $this->add_error_message("Unknown config type '"
              . htmlentities($this->config[$config_key]['value_type']) 
	      . " for '".htmlentities($config_key) . "'");
         $this->setParam($field, $this->getConfigValue($config_key));
      }
   }

   /**
    * Tests if the 'error' parameter has been set.
    * @return true if 'error' parameter not empty, false otherwise
    */ 
   public function has_errors()
   {
      return !empty($this->params['error']);
   }

   /**
    * Gets the HTML error list.
    * @return HTML list of errors
    */ 
   public function get_error_list()
   {
      return sprintf('<ul>%s</ul>', $this->params['error']);
   }

   /**
    * Writes the list of error messages. Use has_errors() first to
    * check if the error parameter has been set.
    */ 
   public function print_errors()
   {
      echo $this->passConfig->getErrorsHaveOccurredMessage(), "<ul>", 
        $this->params['error'], "</ul>";

      if ($this->isUserRole(self::ROLE_ADMIN) && $this->isDebugMode(2))
      {
         echo '[DEBUG (admin)] parameters:<pre>', htmlentities(print_r($this->params, true)), '</pre>';
      }
   }

   /**
    * Clears the error parameter.
    */ 
   public function clear_errors()
   {
      unset($this->params['error']);
   }

   /**
    * Adds a message item to the error parameter list and optionally
    * set an error tag on a parameter field.
    * The message is added with the 'li' element to make it easier
    * to print a summary of errors. Ensure that the message item and
    * error tag are valid HTML.
    * @param $messageitem the message item for the summary list
    * @param $field if not null, add the error tag to this field
    * @param $msg error tag for the field
    */ 
   public function add_error_message($messageitem, $field=null, $msg=null)
   {
      if (isset($this->params['error']))
      {
         $this->params['error'] .= "<li>$messageitem</li>";
      }
      else
      {
         $this->params['error'] = "<li>$messageitem</li>";
      }

      if (isset($field) && isset($msg))
      {
         $fielderror = $field.'error';

         if (isset($this->params[$fielderror]) && $this->params[$fielderror] !== '')
         {
            $this->params[$fielderror] .= "; $msg";
         }
         else
         {
            $this->params[$fielderror] = $msg;
         }
      }
   }

   /**
    * Adds an invalid field value error item to error summary
    * and adds invalid tag to the field's error parameter.
    * @param $field the field name
    * @param $info additional information to add, if not null
    */ 
   public function addInvalidFieldError(string $field, $info=null)
   {
      if (isset($info))
      {
         $this->add_error_message(
             $this->passConfig->getInvalidText($field) . " ($info)",
            $field, $this->passConfig->getInvalidTag());
      }
      else
      {
         $this->add_error_message($this->passConfig->getInvalidText($field),
            $field, $this->passConfig->getInvalidTag());
      }
   }

   /**
    * Checks that a field has been set. If the field parameter
    * hasn't been set or has been set to the empty string then this
    * will add an error message (if an error hasn't already been
    * raised on this field). If an error has already been flagged
    * for the given field, then that will be why it hasn't been set.
    * @param $field the parameter name
    * @param $name the name to use in the error message (if null,
    * use the parameter name)
    */ 
   public function check_not_empty($field, $name=null)
   {
      if (isset($this->params[$field.'error']))
      {
         return; // an error has already been flagged for this field
      }

      if (!isset($this->params[$field]) || $this->params[$field] === '')
      {
         $this->add_error_message(
            $this->passConfig->getMissingText(isset($name) ? $name : $field), $field,
            $this->passConfig->getMissingTag());
      }
   }

   /**
    * Writes the field error tag if set. Use this function next to
    * the corresponding input field where you want the error message
    * to show. Does nothing if no error has been set for the
    * parameter.
    * @param $field the field name
    */ 
   public function element_error_if_set($field)
   {
      if (isset($this->params[$field."error"]))
      {
         echo "<span class=\"error\"><sup>*</sup>", $this->params[$field."error"], "</span>";
      }
   }

   /**
    * Writes the HTML code to start a form.
    * The default action is the current script, the default method
    * is 'post' and the default charset is utf-8.
    * @param $attrs the element attributes or null for defaults
    */ 
   public function start_form($attrs=null)
   {
      if (!isset($attrs))
      {
         $attrs = array();
      }

      if (!isset($attrs['action']))
      {
          $attrs['action'] = $_SERVER['SCRIPT_NAME'];
      }

      if (!isset($attrs['method']))
      {
          $attrs['method'] = "post";
      }

      if (!isset($attrs['accept-charset']))
      {
          $attrs['accept-charset'] = 'utf-8';
      }

      echo "<form";

      foreach ($attrs as $key => $value)
      {
         echo " $key=\"", addslashes($value), "\""; 
      }

      echo ' >';
   }

   /**
    * Writes HTML code for a "Previous" button.
    * @param $attrs the element attributes or null for defaults
    */ 
   public function form_prev_button($attrs=null)
   {
      if (!isset($attrs))
      {
         $attrs = array();
      }

      if (!isset($attrs['type']))
      {
         $attrs['type'] = 'submit';
      }

      if (!isset($attrs['name']))
      {
         $attrs['name'] = 'action';
      }

      if (!isset($attrs['formnovalidate']))
      {
         $attrs['formnovalidate'] = 'formnovalidate';
      }

      if (!isset($attrs['formenctype']))
      {
         $attrs['formenctype'] = 'application/x-www-form-urlencoded';
      }

      echo '<button', process_attributes($attrs), '>', $this->getPrevLabel(),
	   '</button>';
   }

   /**
    * Writes HTML code for a "Next" button.
    * @param $attrs the element attributes or null for defaults
    */ 
   public function form_next_button($attrs=null)
   {
      if (!isset($attrs))
      {
         $attrs = array();
      }

      if (!isset($attrs['type']))
      {
         $attrs['type'] = 'submit';
      }

      if (!isset($attrs['name']))
      {
         $attrs['name'] = 'action';
      }

      echo '<button', process_attributes($attrs), '>', $this->getNextLabel(),
	   '</button>';
   }

   /**
    * Writes HTML code for a "Submit" button.
    * @param $attrs the element attributes or null for defaults
    * @param $text the button text
    */ 
   public function form_submit_button($attrs=null, $text='Submit')
   {
      if (!isset($attrs))
      {
         $attrs = array();
      }

      if (!isset($attrs['type']))
      {
         $attrs['type'] = 'submit';
      }

      if (!isset($attrs['name']))
      {
         $attrs['name'] = 'action';
      }

      echo '<button', process_attributes($attrs), '>', $text,
	   '</button>';
   }

   /**
    * Writes HTML code for a "Cancel" button.
    * @param $attrs the element attributes or null for defaults
    * @param $text the button text
    */ 
   public function form_cancel_button($attrs=null, $text='Cancel')
   {
      if (!isset($attrs))
      {
         $attrs = array();
      }

      if (!isset($attrs['type']))
      {
         $attrs['type'] = 'submit';
      }

      if (!isset($attrs['name']))
      {
         $attrs['name'] = 'action';
      }

      if (!isset($attrs['value']))
      {
         $attrs['value'] = 'cancel';
      }

      if (!isset($attrs['formnovalidate']))
      {
         $attrs['formnovalidate'] = 'formnovalidate';
      }

      echo '<button', process_attributes($attrs), ">$text</button>";
   }

   /**
    * Writes the HTML code for a hidden input element.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    * @param $key the field key if the given parameter is an array
    */ 
   public function form_input_hidden($name, $attrs=null, $key=null)
   {
      return $this->form_input('hidden', $name, $attrs, $key);
   }

   /**
    * Writes the HTML code for a numeric input element.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    * @param $key the field key if the given parameter is an array
    */ 
   public function form_input_number($name, $attrs=null, $key=null)
   {
      return $this->form_input('number', $name, $attrs, $key);
   }

   /**
    * Writes the HTML code for a text input element.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    * @param $key the field key if the given parameter is an array
    */ 
   public function form_input_textfield($name, $attrs=null, $key=null)
   {
      return $this->form_input('text', $name, $attrs, $key);
   }

   /**
    * Writes the HTML code for a datetime-local input element
    * where the value must be in the format: 'Y-m-d\TH:i' or 'Y-m-d H:i'.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    * @param $key the field key if the given parameter is an array
    */ 
   public function form_input_datetime($name, $attrs=null, $key=null)
   {
      if ($attrs == null)
      {
         $attrs = array();
      }

      if (!isset($attrs['pattern']))
      {
         $attrs['pattern'] = "[0-9]{4}-[0-9]{2}-[0-9]{2}[T ][0-9]{2}:[0-9]{2}(:[0-9]{2})?";
      }

      return $this->form_input('datetime-local', $name, $attrs, $key);
   }

   /**
    * Writes the HTML code for a fallback datetime input element
    * where the value must be in the format: 'Y-m-d H:i'.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    *
    * There's only limited browser support for datetime-local so this provides
    * a fallback (which needs to be used with styles/datetimefallback.js,
    * which will automatically be input at the end of the page). The
    * JavaScript will replace the datetime-local element with the
    * fallback code.
    *
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    * @param $key the field key if the given parameter is an array
    */ 
   public function date_time_with_fallback($field, $label=null, $attrs=null, $key=null)
   {
      echo "<span class=\"nativeDateTimePicker\" id=\"nativedatetimepicker-$field\">";

      if (isset($label))
      {
         echo "<label for=\"$field\">$label</label>";
      }

      if (!isset($attrs))
      {
         $attrs = array();
      }

      if (!isset($attrs['placeholder']))
      {
         $placeholder = 'YYYY-HH-DD hh:mm:ss';
	 $attrs['placeholder'] = $placeholder;
      }
      else
      {
	 $placeholder = $attrs['placeholder'];
      }

      if (!isset($attrs['id']))
      {
         $attrs['id'] = $field;
      }

      echo $this->form_input_datetime($field, $attrs, $key);

      echo '</span>';
      $this->date_time_selector($field, $placeholder, $label);
   }

   /**
    * Tests if the fallback datetime JavaScript needs to be input.
    * This needs to be provided as a fallback in the event that the
    * browser doesn't support datetime-local.
    * @return true if the current page requires datetime input,
    * false otherwise
    */ 
   public function isDateTimeFallbackRequired()
   {
      return $this->supplyDateTimeFallback;
   }

   /**
    * The fallback datetime selector.
    * @param $field the parameter name
    * @param $placeholder the placeholder text (which needs to
    * include 'YYYY' if a date selector is needed and 'hh' if a time
    * selector is needed)
    * @param $label the text to label the input or null to use
    * default.
    */ 
   public function date_time_selector($field, $placeholder, $label=null)
   {
      $this->supplyDateTimeFallback = true;
?>
<span class="fallbackDateTimePicker">
<?php 
      if (strpos($placeholder, 'YYYY') !== false)
      {
         $this->passConfig->fallbackDatePicker($field, $label);
         $label = null;
      }

      if (strpos($placeholder, 'hh') !== false)
      {
?>
<label><?php if (isset($label)) { echo $label; $label = null;} else { echo 'Time:'; }?>
<input type="number" placeholder="hh" pattern="\d{1,2}" min="0" max="23" size="2" id="<?php echo $field; ?>-hour">:<input type="number" placeholder="mm" pattern="\d{1,2}" min="0" max="59" size="2" id="<?php echo $field; ?>-minute"><?php 
         if (strpos($placeholder, 'ss') !== false)
         {
?>
:<input type="number" min="0" max="59" placeholder="ss" pattern="\d{1,2}" size="2" id="<?php echo $field; ?>-second">
<?php
         }
?>
</label>
<?php
      }
?>
</span>
<?php
   }

   /**
    * Writes the HTML code for a date input element.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    * @param $key the field key if the given parameter is an array
    */ 
   public function form_input_date($name, $attrs=null, $key=null)
   {
      if ($attrs == null)
      {
         $attrs = array();
      }

      if (!isset($attrs['pattern']))
      {
         $attrs['pattern'] = "[0-9]{4}-[0-9]{2}-[0-9]{2}";
      }

      return $this->form_input('date', $name, $attrs, $key);
   }

   /**
    * Writes the HTML code for a time input element.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    * @param $key the field key if the given parameter is an array
    */ 
   public function form_input_time($name, $attrs=null, $key=null)
   {
      if ($attrs == null)
      {
         $attrs = array();
      }

      if (!isset($attrs['pattern']))
      {
         $attrs['pattern'] = "[0-9]{2}:[0-9]{2}(:[0-9]{2})?";
      }

      return $this->form_input('time', $name, $attrs, $key);
   }

   /**
    * Writes the HTML code for a configuration value.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    * @param $config_key the configuration key (if null, same as the
    * parameter name)
    */ 
   public function form_input_config($name, $attrs=null, $config_key=null)
   {
      if (!isset($config_key))
      {
         $config_key = $name;
      }

      if (!isset($this->config[$config_key]))
      {
         error_log("(form_input_config) Unknown config value '$config_key'");
	 return false;
      }

      if (!isset($this->config[$config_key]['value_type']))
      {
         error_log("(form_input_config) Missing value type for config key '$config_key'");
	 return false;
      }

      if (!isset($this->config[$config_key]['value_constraints']))
      {
         error_log("(form_input_config) Missing value constraints for config key '$config_key'");
	 return false;
      }

      if (!isset($attrs))
      {
         $attrs = array();
      }

      if ($this->config[$config_key]['value_type'] === 'int')
      {
	 if (preg_match('/^\[([\-+]?\d*),([\-+]?\d*)\]$/', $this->config[$config_key]['value_constraints'], $matches))
	 {
            if ($matches[1] !== '')
	    {
               $attrs['min'] = (int)$matches[1];
	    }

            if ($matches[2] !== '')
	    {
               $attrs['max'] = (int)$matches[2];
	    }
	 }
	 else
	 {
            error_log('Invalid int constraint ' . $this->config[$config_key]['value_constraints'] . " for configuration key '$config_key'");
	    return false;
	 }

	 return $this->form_input_number($name, $attrs);
      }
      elseif ($this->config[$config_key]['value_type'] === 'match')
      {
         $attrs['pattern'] = substr($this->config[$config_key]['value_constraints'],
           1, strlen($this->config[$config_key]['value_constraints'])-2);

	 return $this->form_input_textfield($name, $attrs);
      }
      elseif ($this->config[$config_key]['value_type'] === 'text')
      {
	 if (preg_match('/^\[(\d*),(\d+)\]$/', $this->config[$config_key]['value_constraints'], $matches))
	 {
            if (!empty($matches[1]))
	    {
               $attrs['minlength'] = (int)$matches[1];
	    }

            $attrs['maxlength'] = (int)$matches[2];
	 }
	 else
	 {
            error_log('Invalid text constraint ' . $this->config[$config_key]['value_constraints'] . "for configuration key '$config_key'");
	    return false;
	 }

	 return $this->form_input_textarea($name, $attrs);
      }
      else
      {
         error_log('Unknown config type ' . $this->config[$config_key]['value_type'] . "for configuration key '$config_key'");
      }

      return false;
   }

   /**
    * Writes the HTML code for a username value.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    * @param $key the field key if the given parameter is an array
    */ 
   public function form_input_username($name, $attrs=null, $key=null)
   {
      if (!isset($attrs))
      {
         $attrs = array();
      }

      if (!isset($attrs['pattern']))
      {
         $attrs['pattern'] = $this->getUserNamePattern();
      }

      return $this->form_input('text', $name, $attrs, $key);
   }

   /**
    * Writes the HTML code for a registration number value.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    */ 
   public function form_input_regnum($name, $attrs=null)
   {
      if (!isset($attrs))
      {
         $attrs = array();
      }

      if (!isset($attrs['pattern']))
      {
         $attrs['pattern'] = $this->getRegNumPattern();
      }

      return $this->form_input('text', $name, $attrs);
   }

   /**
    * Writes the HTML code for a reset token value.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    */ 
   public function form_input_token($name=null, $attrs=null)
   {
      if (!isset($name))
      {
         $name = 'token';
      }

      if (!isset($attrs))
      {
         $attrs = array();
      }

      if (!isset($attrs['pattern']))
      {
         $attrs['pattern'] = self::TOKEN_PATTERN;
      }

      if (!isset($attrs['maxlength']))
      {
         $attrs['maxlength'] = self::TOKEN_MAXLENGTH;
      }

      if (!isset($attrs['autocomplete']))
      {
         $attrs['autocomplete'] = 'off';
      }

      return $this->form_input('text', $name, $attrs);
   }

   /**
    * Writes the HTML code for a password value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    */ 
   public function form_input_password($name, $attrs=null)
   {
      if (!isset($attrs))
      {
         $attrs = array();
      }

      if (!isset($attrs['minlength']))
      {
         $attrs['minlength'] = self::MIN_PASSWORD_LENGTH;
      }

      return $this->form_input('password', $name, $attrs);
   }

   /**
    * Writes the HTML code for a generic value.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $type the element type
    * @param $inputname the parameter name
    * @param $attrs the element attributes or null for defaults
    * @param $key the field key if the given parameter is an array
    */ 
   public function form_input($type, $inputname, $attrs=null, $key=null)
   {
      $name = preg_replace('/\[\]$/', '', $inputname);

      if (!isset($key) && $this->isParamArray($name))
      {
         $result = '';

         if (!preg_match('/\[\]$/', $inputname))
	 {
            $inputname .= '[]';
	 }

         foreach ($this->getParam($name) as $arrkey=>$val)
	 {
            $result .= $this->form_input($type, $inputname, $attrs, $arrkey);
	 }

         return $result;
      }

      $text = "<input type=\"$type\" name=\"$inputname\""
            . process_attributes($attrs);

      if (isset($this->params[$name.'error']) && isset($_POST[$name]))
      {
         if ($type === 'password')
         {// don't write value
         }
         elseif (is_array($_POST[$name]))
         {
            $value = htmlentities($_POST[$name][$key], ENT_COMPAT | ENT_HTML401 | ENT_QUOTES);
         }
         else
         {
            $value = htmlentities($_POST[$name], ENT_COMPAT | ENT_HTML401 | ENT_QUOTES);
         }
      }
      elseif (isset($this->params[$name]))
      {
         if (is_array($this->params[$name]))
         {
            if (isset($this->params[$name][$key]))
	    {
               $paramvalue = $this->params[$name][$key];
	    }
         }
         else
         {
            $paramvalue = $this->params[$name];
         }

	 if (!isset($paramvalue))
	 {// array element not set
	 }
	 elseif ($this->params[$name] instanceof DateTime)
         {
            if ($type == 'date')
            {
               $value = date_format($paramvalue, 'Y-m-d');
            }
            elseif ($type == 'time')
            {
               if (isset($attrs) && isset($attrs['placeholder']) 
                    && $attrs['placeholder']==='hh:mm:ss')
	       {
                  $value = date_format($paramvalue, 'H:i:s');
	       }
	       else
	       {
                  $value = date_format($paramvalue, 'H:i');
	       }
            }
            else
            {
               if (isset($attrs) && isset($attrs['placeholder'])
                    && strpos($attrs['placeholder'], 'hh:mm:ss') !== false)
	       {
                  $value = date_format($paramvalue, 'Y-m-d\TH:i:s');
	       }
	       else
	       {
                  $value = date_format($paramvalue, 'Y-m-d\TH:i');
	       }
            }

            $value = htmlentities($value, ENT_COMPAT | ENT_HTML401 | ENT_QUOTES);
         }
         else
         {
            $value = $paramvalue;

            if (preg_match('/&#[0-9A-Fa-f]+;|&[0-9]+;|&[a-zA-Z]+;|[<>]/', $value))
            {
               $value = html_entity_decode($value, ENT_COMPAT | ENT_HTML401 | ENT_QUOTES);
            }

            $value = htmlentities($value, ENT_COMPAT | ENT_HTML401 | ENT_QUOTES);
         }
      }

      if (isset($value))
      {
         $text .= " value=\"$value\" ";
      }

      $text .= '>';
   
      return $text;
   }

   /**
    * Writes the HTML code for a text area value.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    * @param $encode_entities if true encode the value
    */ 
   public function form_input_textarea($name, $attrs=null, $encode_entities=true)
   {
      $text = "<textarea name=\"$name\"" . process_attributes($attrs);

      $text .= '>';

      if (isset($this->params[$name.'error']) && isset($_POST[$name]))
      {// always encode $_POST value
         $text .= htmlentities($_POST[$name]);
      }
      elseif (isset($this->params[$name]))
      {
         if ($encode_entities)
         {
            $text .= htmlentities($this->params[$name]);
         }
         else
         {
            $text .= $this->params[$name];
         }
      }

      return "$text</textarea>";
   }

   /**
    * Writes the HTML code for a checkbox.
    * If the parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    */ 
   public function form_input_checkbox($name, $attrs=null)
   {
      if (!isset($attrs))
      {
         $attrs = array();
      }

      $isarray = false;
      $attrs['name'] = $name;
      $attrs['type'] = 'checkbox';

      if (substr($name, -2) === '[]')
      {
         $name = substr($name, 0, strlen($name)-2);
         $isarray = true;
      }
      elseif (is_array($this->params[$name]))
      {
         $attrs['name'] = $name.'[]';
         $isarray = true;
      }

      if (isset($attrs['value']))
      {
         $value = $attrs['value'];
      }

      if ((isset($value) && isset($this->params[$name]))
        && (
             ($isarray && is_array($this->params[$name])
                 && in_array($value, $this->params[$name]))
           || $this->params[$name] === $value
	))
      {
         $attrs['selected'] = 'selected';
      }

      if (!isset($attrs['value']))
      {
         if (isset($value))
         {
            $attrs['value'] = $value;
         }
         elseif (isset($this->params[$name]))
         {
            $attrs['value'] = $this->params[$name];
         }
      }

      return html_void_element('input', $attrs);
   }

   /**
    * Writes the HTML code for a boolean checkbox.
    * If the (boolean) parameter with the given name has been set, that will
    * be used as the value.
    * @param $name the parameter name
    * @param $default the default value (empty for false)
    * @param $attrs the element attributes or null for defaults
    */ 
   public function form_input_boolean_checkbox($name, $default=null, $attrs=null)
   {
     if (!isset($this->params[$name]))
     {
        $selected = (empty($default) ? false : $default);
     }
     else if ($this->params[$name] === false || $this->params[$name] === 'off')
     {
        $selected = false;
     }
     elseif ($this->params[$name] === true || $this->params[$name] === 'on')
     {
        $selected = true;
     }
     elseif ($default === null)
     {
        $selected = false;
     }
     else
     {
        $selected = $default;
     }

     $text = "<input type=\"checkbox\" name=\"$name\" value=\"on\""
            . process_attributes($attrs);

     if ($selected)
     {
        $text .= "checked ";
     }

     return "$text>";
   }

   /**
    * Writes the HTML code for a user role selector.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    */ 
   public function form_input_role($name, $attrs=null)
   {
      return $this->form_input_select($name, self::ROLE_OPTIONS, '', '', false, $attrs);
   }

   /**
    * Writes the HTML code for a user status selector.
    * @param $name the parameter name
    * @param $attrs the element attributes or null for defaults
    */ 
   public function form_input_status($name, $attrs=null)
   {
      return $this->form_input_select($name, self::STATUS_OPTIONS, '', '', false, $attrs);
   }

   /**
    * Writes the HTML code for a generic selector.
    * If $usekey is true, the key from the $validvalues array will
    * be used as the option value and the array element value will be used
    * as the textual part. Otherwise the value will be the same for
    * both.
    * The 'any' value, if set, will have a value of -1 if $usekey is
    * true otherwise its value will be $any.
    * @param $name the parameter name
    * @param $validvalues array of valid values
    * @param $default the default value
    * @param $any the value indicating "any" or "nothing selected" option
    * @param $usekey if true, use the key from the valid values
    * otherwise use the array element value as the input value
    * @param $attrs the element attributes or null for defaults
    * @param $valuefield the key to use for the value if the valid
    * value element is an array
    */ 
   public function form_input_select($name, $validvalues, $default='', $any='', $usekey=false, $attrs=null, $valuefield=null)
   {
     if (isset($this->params[$name]))
     {
        $selected = (string)$this->params[$name];
     }
     elseif (empty($default) && $any)
     {
        $selected = $any;
     }
     else
     {
        $selected = $default;
     }
   
     $text = "<select name=\"$name\" " . process_attributes($attrs);
   
     $text .= ">\n";
   
     if ($any)
     {
        $text .= '<option value="' . ($usekey ? -1 : $any). '" ';
   
        if ($selected === $any)
        {
          $text .= "selected";
   
          $text .= ' ';
        }
   
        $text .= ">$any</option>\n";
     }

     foreach ($validvalues as $key => $value)
     {
       $text .= "<option ";
   
       $selectedvalue = sprintf('%s', $usekey ? $key : $value);
   
       if (is_array($value))
       {
          if (isset($valuefield))
          {
             $value = $value[$valuefield];
          }
          else
          {
             $value = $value[0];
          }
       }
   
       if ($selected === $selectedvalue)
       {
          $text .= "selected ";
       }
   
       $text .= 'value="' . ($usekey ? $key : $value) . "\">$value</option>\n";
     }

     return "$text</select>";
   }

   /**
    * Deletes the given directory and its contents recursively.
    * @param $dir the directory
    * @return true if successful, false otherwise
    */ 
   private function delTree($dir)
   {
      $success = true;

      $files = scandir($dir);
      
      if ($files !== false)
      {
         foreach ($files as $filename)
         {
            if ($filename !== '.' && $filename !== '..')
            {
               $f = "$dir/$filename";
      
               if (is_dir($f))
               {
      		  if (delTree($f) === false)
      		  {
                     $success = false;
                  }
      	       }
   	       elseif (!unlink($f))
  	       {
                  if (!is_writable($f))
                  {
                     $this->log_error("Unable to delete '$f'. No write access.");
                  }
                  else
                  {
                     $this->log_error("Unable to delete '$f'.");
                  }

		  $success = false;
               }
            }
         }
      }
      
      if (!rmdir($dir))
      {
         if (!is_writable($dir))
         {
            $this->log_error("Unable to delete '$dir'. No write access.");
         }
         else
         {
            $this->log_error("Unable to delete '$dir'.");
         }

         $success = false;
      }

      return $success;
   }
   
   /**
    * Deletes the upload directory and its contents recursively.
    * Admin only function.
    * @param $dirbasename the upload directory basename
    * @return true if successful, false otherwise
    */ 
   public function delUploadDir($dirbasename)
   {
      if (!$this->isUserRole(self::ROLE_ADMIN))
      {
         $this->add_error_message ('Function \'delUploadDir\' not available (admin only).');
         return false;
      }
   
      $dir = $this->getUploadPath()."/$dirbasename";
   
      if (!file_exists($dir))
      {
         $this->add_error_message('No such upload name: \'' . htmlentities($dirbasename) . '\'');
         return false;
      }
   
      return $this->delTree($dir);
   }

   /**
    * Gets the pagination data.
    * @return an array containing the pagination data:
    * 'page' (current page), 'start_idx' (start index), 'end_idx'
    * (end index), 'max_items_per_page' (maximum items per page),
    * and 'num_pages' (number of pages).
    */ 
   public function getListPages(int $num_items, $current_page = null)
   {
      if (!isset($current_page) && $this->isParamSet('page')
           && is_numeric($this->getParam('page')))
      {
         $current_page = (int)$this->getParam('page');
      }

      if (empty($current_page) || !is_numeric($current_page))
      {
         $current_page = 1;
      }

      $max_items_per_page = (int)$this->getConfigValue('max_items_per_page');
      $start_idx = 0;
      $end_idx = $num_items-1;
      $num_pages = 1;

      if ($num_items > $max_items_per_page)
      {
         $num_pages = (int)ceil($num_items/$max_items_per_page);

         if ($current_page < 1)
         {
	    $current_page = 1;
         }
	 elseif ($current_page > $num_pages)
         {
	    $current_page = $num_pages;
         }

         $start_idx = ($current_page-1)*$max_items_per_page;
         $end_idx = min($num_items, $start_idx+$max_items_per_page);
      }

      return array('page'=>$current_page, 'start_idx'=>$start_idx,
	      'end_idx'=>$end_idx, 'max_items_per_page'=>$max_items_per_page,
              'num_pages'=>$num_pages);
   }

   /**
    * Returns the pagination list.
    * @param $num_pages the number of pages
    * @param $query_str the query string to add to each page link
    * @param $current_page the current page number
    * @return HTML code
    */ 
   public function page_list($num_pages, $query_str=null, $current_page=null)
   {
      if ($num_pages < 2) return '';

      if (empty($current_page) || !is_numeric($current_page))
      {
         if (empty($this->getParam('page')) || !is_numeric($this->getParam('page')))
	 {
            $current_page = 1;
	 }
	 else
	 {
            $current_page = (int)$this->getParam('page');
	 }
      }

      if (empty($query_str))
      {
         $query_str = '';
      }
      else
      {
         $query_str .= '&amp;';
      }

      $result = '<div class="page_list">';

      if ($current_page === 1)
      {
         $result .= '<span class="page_list">1</span>';
      }
      else
      {
         $result .= $this->href_self("${query_str}page=1", 
		      '&#x23EE;', ['title'=>'first page', 'class'=>'first_page']);
         $result .= $this->href_self("${query_str}page=". ($current_page-1), 
		      '&#x23F4;', ['title'=>'previous page', 'class'=>'prev_page']);
         $result .= ' ' . $this->href_self("${query_str}page=1", '1', ['class'=>'page']);
      }

     if ($num_pages > 2)
     {
        $start_idx = 2;
        $end_idx = $num_pages-1;

        if ($num_pages > 8)
        {
           if ($current_page > 8)
	   {
              $start_idx = $current_page-5;
	   }

	   if ($current_page <= $num_pages - 8)
	   {
              $end_idx = $current_page+5;
	   }
        }

	if ($start_idx > 2)
	{
           $result .= ' ...';
	}

        for ($i = $start_idx; $i <= $end_idx; $i++)
        {
           if ($i === $current_page)
	   {
              $result .= " <span class=\"page\">$i</span>";
	   }
	   else
	   {
	      $result .= ' ' . $this->href_self("${query_str}page=$i", $i,
			   ['class'=>'page']);
	   }
        }

	if ($end_idx < $num_pages-1)
	{
           $result .= ' ...';
	}
     }

     $result .= ' ';

     if ($current_page === $num_pages)
     {
        $result .= "<span class=\"page\">$num_pages</span>";
     }
     else
     {
        $result .= $this->href_self("${query_str}page=$num_pages", $num_pages, 
		     ['class'=>'page']) . ' ';
        $result .= $this->href_self("${query_str}page=". ($current_page+1), 
		     '&#x23F5;', ['title'=>'next page', 'class'=>"next_page"]) . ' ';
        $result .= $this->href_self("${query_str}page=$num_pages", 
		     '&#x23ED;', ['title'=>'last page', 'class'=>"last_page"]);
     }

     return "$result</div>";
   }

   /**
     * Requires current user to be authenticated. If the user isn't logged in
     * this will redirect to the login page. If a user with 2FA
     * enabled has had their password verified but has not yet
     * passed the second step, this function will redirect to the
     * 2FA page. If the user hasn't yet verified their account, this
     * will redirect to the account verification page.
     * If the user's account is blocked, this will write
     * the error page and exit.
     *
     * This function must be called before any content is written to
     * STDOUT otherwise the redirect will fail.
     */
   public function require_login()
   {
      if (!isset($this->user) || !isset($this->user['id']))
      {
         $this->redirect_login_header();
         exit;
      }

      if (isset($this->user['status']))
      {
         if ($this->user['status'] === self::STATUS_BLOCKED)
         {
            $this->logout();
            $this->pageTitle = 'User Blocked';
            $this->page_header();
?>
<p>This account has been blocked. Please contact <?php 

            echo $this->getConfigValue('help_reference');

?> for assistance.
<?php
            $this->page_footer();
            exit;
         }

         if ($this->user['status'] === self::STATUS_PENDING)
         {
            $this->redirect_header($this->getVerifyAccountRef());
            exit;
         }
      }
      elseif ($this->isUserVerificationRequired())
      {
         $this->redirect_header($this->getCheckMultiFactorRef(), true);
         exit();
      }
   }

   /**
     * Requires current user to be authenticated and be an 'admin' user.
     * This function starts by ensuring the user is logged in with
     * require_login.
     *
     * If the user is authenticated but not admin, this will write
     * the error page and exit.
     *
     * This function must be called before any content is written to
     * STDOUT otherwise the redirect will fail.
     */
   public function require_admin()
   {
      $this->require_login();

      if ($this->user['role'] !== self::ROLE_ADMIN)
      {
         $this->pageTitle = 'Prohibited Area';
         $this->page_header();
?>
<p>You don't have permission to access this area.
<?php
         $this->page_footer();
         exit();
      }
   }

   /**
     * Checks the site mode. If maintenance mode and not admin or test
     * user or on the login, create account, verify account or password
     * reset pages, this redirects to the maintenance page and exits.
     * The login page (and 2fa page) is needed to allow admin or test user to log in.
     * It's possible for other users to login if they have the login
     * page bookmarked, but they will then be redirected to the
     * maintenance page. A banner will be shown at the top of each
     * page that indicates the site is in maintenance mode.
     *
     * This function must be called before any content is written to
     * STDOUT otherwise the redirect will fail.
     *
     * @return true if maintenance mode, false otherwise
    */ 
   public function check_site_mode()
   {
      $is_maintenance = ($this->getConfigValue('mode')===1 ? true : false);

      $allow_test_user = ($this->getConfigValue('allow_test_user')===1 ? true : false);

      if ($is_maintenance 
         && !($_SERVER['PHP_SELF'] === $this->getLoginRef()
              || $_SERVER['PHP_SELF'] === $this->getCheckMultiFactorRef()
              || $_SERVER['PHP_SELF'] === $this->getForgottenPasswordRef()
              || $_SERVER['PHP_SELF'] === $this->getResetPasswordRef()
              || $this->isUserRole(self::ROLE_ADMIN)
              || ($allow_test_user && ($this->isTestUser()
                  || $_SERVER['PHP_SELF'] === $this->getCreateAccountRef()
                  || $_SERVER['PHP_SELF'] === $this->getVerifyAccountRef()
                  || $_SERVER['PHP_SELF'] === $this->getResendVerifyRef()
              ))))
      {
         header("Location: " . PassConfig::MAINTENANCE_HREF);
         exit;
      }

      return $is_maintenance;
   }

   /**
    * Writes the page header.
    */ 
   public function page_header()
   {
      $this->passConfig->page_header();
   }

   /**
    * Writes the page footer.
    */ 
   public function page_footer()
   {
      $this->passConfig->page_footer();
   }

   /**
    * Writes the JavaScript code to implement collapsible content.
    * @param $includeTags if true, write the 'script' tags as well
    */ 
   public function writeCollapsibleJavaScript($includeTags=true)
   {
      if ($includeTags)
      {
?>
<script>
<?php
      }
?>
var coll = document.getElementsByClassName("collapsible");
var i;

for (i = 0; i < coll.length; i++)
{
   coll[i].addEventListener("click", function()
     {
       this.classList.toggle("active");
       var content = this.nextElementSibling;
       if (content.style.display === "block")
       {
         content.style.display = "none";
       }
       else
       {
         content.style.display = "block";
       }
     }
   );
}
<?php
      if ($includeTags)
      {
?>
</script>
<?php
      }
   }
}

?>
