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

 * Terms of use page. I'm not a legal expert. This is an example.
 * Change as applicable.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Terms of Use');

$pass->page_header();

?>
   <p>This terms of use policy (together with the documents referred to in it)
   tells you the terms of use on which you may make use of the website
   <?php echo PassConfig::WEB_DOMAIN; ?> (<span class="legalterm">this
   site</span>), whether as a guest or a registered user. Use of this
   site includes accessing, browsing, or registering to use this
   site.</p>

   <p>Please read these terms of use carefully before you start to use
   this site, as these will apply to your use of this site. We recommend
   that you print or save a copy of this for future reference. </p>

   <p>By using this site, you confirm that you accept these terms of use
   and that you agree to comply with them. </p>

   <p>If you do not agree to these terms of use, you must not use this
   site.</p>

   <p>This site is only provided for staff and students in the 
   <?php echo PassConfig::SCHOOL_NAME; ?> (<span class="legalterm"><?php echo PassConfig::SCHOOL_SHORT; ?></span>) at the <?php echo PassConfig::UNIVERSITY_LONG_NAME; ?>.

   <h2>Other applicable terms</h2>
   <p>These terms of use refer to the following additional terms, which
   also apply to your use of this site:</p>

   <ul>
     <li><?php echo $pass->get_legal_link(); ?></li>
   </ul>

   <section>
   <h2>Changes to these terms and this site</h2>
   <p>We may revise these terms of use at any time by amending this
   page. Please check this page from time to time to take notice of any
   changes we made, as they are binding on you. </p>

   <p>We may update this site from time to time, and may change the
   content at any time. However, please note that any of the content on
   this site may be out of date at any given time, and we are under no
   obligation to update it. </p>

   </section>

   <section>
   <h2>Accessing this site</h2>
   <p>We do not guarantee that this site, or any content on it, will
   always be available or be uninterrupted. We may suspend, withdraw,
   discontinue or change all or any part of this site without notice. We
   will not be liable to you if for any reason this site is unavailable
   at any time or for any period.</p>

   <p>You are also responsible for ensuring that all persons who access
   this site through your internet connection are aware of these terms
   of use and other applicable terms and conditions, and that they
   comply with them.  </p>
   </section>

   <section>
   <h2>Preparing Assignments for Submission</h2>
   <p>This site provides a tool to help you prepare your programming
   assignments for submission. It's your responsibility to check that the PDF
   created by this tool accurately represents your assignment. It's your
   responsibility to submit the PDF in a timely manner through 
   <a href="<?php echo PassConfig::SUBMISSION_SITE_HREF; ?>"><?php echo PassConfig::SUBMISSION_SITE; ?></a>.

   <p>Any content standards below apply to any and all material uploaded to 
   this site (including source code and associated documents). You must comply
   with the spirit of the standards as well as the letter. The
   standards apply to each part of any contribution as well as to its
   whole.</p>

   <p>Content must follow standard <?php echo PassConfig::SCHOOL_SHORT; ?> practice.</p>

   <p>Content must not: contain any material which is defamatory
   of any person; promote discrimination based on race, sex, religion,
   nationality, disability, sexual orientation or age; infringe any
   copyright, database right or trade mark of any other person; be
   likely to deceive any person; be made in breach of any legal duty
   owed to a third party, such as a contractual duty or a duty of
   confidence; or advocate, promote or assist any unlawful act such as
   (by way of example only) copyright infringement or computer
   misuse.</p>

   </section>

   <section>
   <h2>Limitation of our liability</h2>
   <p><strong>YOUR ATTENTION IS DRAWN IN PARTICULAR TO THIS CLAUSE</strong></p>

   <p>Nothing in these terms of use excludes or limits our liability
   for death or personal injury arising from our negligence, or our
   fraud or fraudulent misrepresentation, or any other liability that
   cannot be excluded or limited by English law.</p>

   <p>To the extent permitted by law, we exclude all conditions,
   warranties, representations or other terms which may apply to this
   site or any content on it, whether express or implied. </p>

   <p>We will not be liable to any user for any loss or damage, whether
   in contract, tort (including negligence), breach of statutory duty,
   or otherwise, even if foreseeable, arising under or in connection
   with:</p>

   <ul>
   <li>use of, or inability to use, this site; or</li>

   <li>use of or reliance on any content displayed on this site. </li>

   </ul>

   <p>Please note that we only provide this site for students in the 
   <?php echo PassConfig::SCHOOL_NAME; ?> at the <?php echo PassConfig::UNIVERSITY_LONG_NAME; ?>. You agree not to use this
   site for any commercial or business purposes, and we have no liability to you
   for any loss of profit, loss of business, business interruption, or loss of
   business opportunity.</p>

   <p>We will not be liable for any loss or damage caused by a virus,
   distributed denial-of-service attack, or other technologically
   harmful material that may infect your computer equipment, computer
   programs, data or other proprietary material due to your use of this
   site or to your downloading of any content on it, or on any website
   linked to it.</p>
   </section>

   <section>
   <h2>Applicable law</h2>

   <p>Please note that these terms of use, its subject matter and its
   formation, are governed by English law. You and we both agree to
   that the courts of England and Wales will have non-exclusive
   jurisdiction. </p>
   </section>

<?php
$pass->page_footer();
?>
