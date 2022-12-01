<?php
/*
 * Server PASS
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

 * Template page.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

const ALLOWED_TAGS='<strong><em><code>';

$pass = new Pass('Template Page');

/*
 * If user isn't logged in or is blocked or requires verification
 * the following will redirect to the login page (with ?from=this_page.php appended
 * so user will return back here after logging in) or other relevant page.
 */

//$pass->require_login();// ensure user is logged in
$pass->require_admin();// alternatively, ensure admin user is logged in

// process some page parameters
process_params();

if ($pass->isParam('action', 'redirect'))
{// direct to another page with a message to display at the top of the target page.
   $_SESSION['confirmation_message']='Redirected from template page.';

   // if argument omitted, defaults to /index.php
   $pass->redirect_header($pass->getAccountRef());
}
else
{
   $pass->page_header();

   if ($pass->has_errors())
   {
      $pass->print_errors();
   }

   if ($pass->isParam('action', 'cancel'))
   {
      echo '<div class="confirmation">Cancelled.</div>';
      show_form();
   }
   elseif ($pass->isParam('action', 'submit'))
   {
      submit();
   }
   elseif ($pass->isParam('action', 'search'))
   {
      search();
   }
   else
   {
      show_form();
   }

   $pass->page_footer();
}

function process_params()
{
   global $pass;

   // 'action' parameter may only have a value that matches this regex, default value provided:
   $pass->get_matched_variable('action', '/^(submit|cancel|search|redirect|showform)$/',
	   'showform');

   // any value allowed but escape any HTML markup (no default value):
   $pass->get_htmlentities_variable('search');

   /*
    * Set up some variables for use by this page that aren't input
    * by form parameters, but may be affected by form parameters
    * (constants could be used instead for this simple example)
    */
   $pass->setParam('animals',
	   array('leopard'=>'Leopard &#x1F406;',
	   'dromedary_camel'=>'Dromedary Camel &#x1F42A;',
	   'bactrian_camel'=>'Bactrian Camel &#x1F42B;',
	   'penguin'=>'Penguin &#x1F427;',
	   't-rex'=>'T-Rex &#x1F996;',
	   'sauropod'=>'Sauropod &#x1F995;',
	   'hippo'=>'Hippopotamus &#x1F99B;'
   ));

   $pass->setParam('weather_list',
            array(
	    'sunny'=>'Sunny &#x1F323;',
            'mostly_sunny'=>'Mostly Sunny &#x1F324;',
            'mostly_cloudy'=>'Mostly Cloudy &#x1F325;',
            'showers'=>'Showers &#x1F326;',
            'rain'=>'Rain &#x1F327;',
            'snow'=>'Snow &#x1F328;',
            'windy'=>'Windy &#x1F32C;',
            'storm'=>'Stormy &#x1F329;',
            'fog'=>'Fog &#x1F32B;',
            'tornado'=>'Tornado &#x1F32A;'
            ));

   if ($pass->isParam('action', 'cancel'))
   {
      return;
   }

   if ($pass->isParam('action', 'search') && $pass->isParamSet('search')
	   && preg_match('/leopard/i', $pass->getParam('search')))
   {
      /*
       * Syntax for add_error_message: 
       *  - message (displayed in $pass->print_errors())
       *  - field label (optional)
       *  - field error tag (optional)
       * If field label and error tag set then the error tag can be displayed
       * next to the field with
       * $pass->element_error_if_set(field_label);
       */
      $pass->add_error_message("You can't search for leopards", 'search', 'Invalid');
   }

   // This parameter value must be an integer, default value provided:
   // Parameter (if set) will be an int
   $pass->get_int_variable('page', 1);

   // This parameter value must be boolean, no default value:
   // Parameter (if set) will be either string 'on' or boolean false
   $pass->get_boolean_variable('agree');

   // This parameter value must match the username regex, no default:
   $pass->get_username_variable('username');

   // This parameter value must match registration number regex, no default:
   $pass->get_regnum_variable('regnum');

   // The parameter value must match one of the items in the given array, no default:
   $pass->get_choice_variable('animal', array_keys($pass->getParam('animals')));

   $pass->get_choice_variable('weather', array_keys($pass->getParam('weather_list')),
     '-1');

   /*
    * The parameter value must match one of the following formats:
    *   Y-m-d\TH:i:s
    *   Y-m-d H:i:s
    *   Y-m-d\TH:i
    *   Y-m-d H:i
    * Default value may be omitted, a string in one of the above formats or a DateTime instance
    * Parameter (if set) will be an instance of DateTime
    */
   $pass->get_datetime_variable('timestamp');

   // As above but format must be Y-m-d
   $pass->get_date_variable('date');

   $pass->get_time_variable('time');

   /*
    * The parameter value may contain allowed HTML tags.
    * All other tags will be stripped and an error will occur:
    */
   $pass->get_striptags_variable('message', ALLOWED_TAGS);

   // required parameters if action=submit:
   if ($pass->isParam('action', 'submit'))
   {
      $pass->check_not_empty('username');

      if ($pass->isParam('weather', -1, false))
      {
         $pass->add_error_message('No weather information selected.', 'weather', 'required');
      }
   }

   if ($pass->has_errors())
   {// one or more errors occurred (missing or invalid parameters)
      $pass->setParam('action', 'showform');
   }
}

function show_form()
{
   global $pass;

   // Start the first form:
   echo $pass->start_form();
?>
<p>
<label for="search_box_id">Search:</label>
<?php
   echo $pass->form_input_textfield('search', ['id'=>'search_box_id']), 
	   $pass->element_error_if_set('search');

   // button that sets 'action' parameter:
   echo $pass->form_submit_button(['value'=>'search'], 'Search &#x1F50D;');
   echo '</form>'; // end of first form

   // Start the second form
   echo $pass->start_form();
   echo $pass->form_submit_button(['value'=>'submit', 'class'=>'visually-hidden', 'tabindex'=>-1]);

?>
<p>
<label for="username_box_id">User name:</label>
<?php
   // sets pattern to username regex
   echo $pass->form_input_username('username', 
	   ['id'=>'username_box_id', 'required'=>true]), 
	   $pass->element_error_if_set('username');
?>
<p>
<label for="regnum_box_id">Registration Number:</label>
<?php
   // sets pattern to registration number regex
   echo $pass->form_input_regnum('regnum', ['id'=>'regnum_box_id']), 
	   $pass->element_error_if_set('regnum');

   echo '<p>';

   // boolean checkbox with default value
   echo $pass->form_input_boolean_checkbox('agree', false, ['id'=>'agree_box_id']);
?>
<label for="agree_box_id">I agree to something.</label>
<p>Which animals do you like?
<?php
   // an array of checkboxes

   foreach ($pass->getParam('animals') as $key=>$value)
   {
      echo '<p>', $pass->form_input_checkbox('animal[]', 
		   ['value'=>$key, 'id'=>"animal_$key"]);
?>
<label for="animal_<?php echo $key;?>"><?php echo $value?></label>
<?php
   }

?>
<p>
<label for="weather_box_id">What's the weather like today?</label>
<?php
   echo $pass->form_input_select('weather', 
      $pass->getParam('weather_list'),// valid values
      '',// default value
      '---',// not listed option
      true, // use array key for option value attribute
      ['id'=>'weather_box_id']), 
      $pass->element_error_if_set('weather');

?>
<p><label for="timestamp">Enter a date and time: </label>
<?php
   // Only a few browsers support datetime-local so provide a fallback.
   $pass->date_time_with_fallback('timestamp');

   echo $pass->element_error_if_set('timestamp');
?>
<p><label for="dateonly">Enter a date:</label>
<?php
   echo $pass->form_input_date('date', ['id'=>'dateonly']);
   echo $pass->element_error_if_set('date');
?>
<p><label for="timeonly">Enter a time:</label>
<?php
   echo $pass->form_input_time('time', ['id'=>'timeonly']);
   echo $pass->element_error_if_set('time');
?>
<p><label for="message_box_id">Message area:</label>
<?php
   echo ' (allowed tags:', htmlentities(ALLOWED_TAGS), ')';

   echo $pass->element_error_if_set('message');

   /*
    * This parameter will be set by $pass->get_striptags_variable()
    * if invalid content has been stripped ('_striptagsdiff'
    * is appended to the corresponding field label).
    */
   if ($pass->isParamSet("message_striptagsdiff"))
   {
      echo '<pre>', $pass->getParam("message_striptagsdiff"), '</pre>';
   }

   echo '<p>', $pass->form_input_textarea('message', 
	   ['id'=>'message_box_id',
	    'maxlength'=>256,
	    'cols'=>20, 'rows'=>6,
	    'placeholder'=>'Type message here.']);

   // hidden parameter
   echo $pass->form_input_hidden('page');

   echo '<p>';

   // button with action=cancel and prevents client-side HTML form validation
   echo $pass->form_cancel_button();

   echo '<span class="spacer"></span>';

   echo $pass->form_submit_button(['value'=>'submit']);

   echo '</form>';
}

function search()
{
   global $pass;

   // HTML markup has already been replaced
   echo '<p>Search Term: ', $pass->getParam('search');
   echo '<p>', $pass->href_self('action=showform', 'Return to form.');
}

function submit()
{
   global $pass;
?>
<p>Form submitted. Username: 
<?php

   /*
    * htmlentities isn't strictly needed here as username regex
    * doesn't allow markup similarly for registration number
    * but use anyway.
    */
   echo htmlentities($pass->getParam('username'));

   echo ' Reg. Num.: ';

   if ($pass->isParamSet('regnum'))
   {
      echo htmlentities($pass->getParam('regnum'));
   }
   else
   {
      echo '<span class="notset">Not set.</span>';
   }

   if ($pass->isBoolParamOn('agree'))
   {
      echo '<p>You agreed to something.';
   }

   if ($pass->isParamArray('animal'))
   {
      echo '<p>You like the following animal(s): ';

      foreach ($pass->getParam('animal') as $key)
      {
         echo '<p>', $pass->getParamElement('animals', $key);
      }
   }
   else
   {
      echo '<p>No animals selected.';
   }

   echo '<p>The weather today is: ', 
	   $pass->getParamElement('weather_list', $pass->getParam('weather'));

   if ($pass->isParamSet('timestamp'))
   {
      echo '<p>Timestamp: ', date_format($pass->getParam('timestamp'), DATE_RSS);
   }
   else
   {
      echo '<p>No timestamp provided.';
   }

   if ($pass->isParamSet('date'))
   {
      echo '<p>Date: ', date_format($pass->getParam('date'), 'D jS F Y');
   }
   else
   {
      echo '<p>No date provided.';
   }

   if ($pass->isParamSet('time'))
   {
      echo '<p>Time: ', date_format($pass->getParam('time'), 'H:i');
   }
   else
   {
      echo '<p>No time provided.';
   }

   echo '<p>Message: ', $pass->getParam('message');

   echo '<p>', $pass->href_self('action=showform', 'Return to form.');
}

?>
