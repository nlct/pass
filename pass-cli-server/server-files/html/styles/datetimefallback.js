checkDateTimeSupport();

function checkDateTimeSupport()
{
   var test = document.createElement('input');
   var dateTimeSupported = false;

   try
   {
     test.type = 'datetime-local';
     dateTimeSupported = (test.type === 'datetime-local');
   }
   catch (e)
   {
   }

   if (dateTimeSupported)
   {
      var elems = document.getElementsByClassName("nativeDateTimePicker");

      if (elems)
      {
         for (var i = 0; i < elems.length; i++)
         {
            elems[i].style.display='inline';
         }
      }

      elems = document.getElementsByClassName("fallbackDateTimePicker");

      if (elems)
      {
         for (var i = 0; i < elems.length; i++)
         {
            elems[i].style.display='none';
         }
      }
   }
   else
   {
      var elems = document.getElementsByClassName("nativeDateTimePicker");

      if (elems)
      {
         for (var i = 0; i < elems.length; i++)
         {
            elems[i].style.display='none';

	    if (elems[i].id)
	    {
	       var found = elems[i].id.match(/^nativedatetimepicker-(.+)$/);

	       if (found)
	       {
                  const mainId = found[1];

		  const nativeElem = document.getElementById(mainId);
	          const dateTimeValue = nativeElem.value;

		  var fallbackDay = document.getElementById(mainId+"-day");
		  var fallbackMonth = document.getElementById(mainId+"-month");
		  var fallbackYear = document.getElementById(mainId+"-year");

		  var fallbackHour = document.getElementById(mainId+"-hour");
		  var fallbackMinute = document.getElementById(mainId+"-minute");
		  var fallbackSecond = document.getElementById(mainId+"-second");

		  if (fallbackDay && fallbackMonth && fallbackYear)
		  {
                     var dayValue = null;
                     var monthValue = null;
                     var yearValue = null;

		     if (dateTimeValue)
		     {
	                found = dateTimeValue.match(/^(\d{4})-(\d{2})-(\d{2})/);

		        if (found)
		        {
                           dayValue = found[3];
                           monthValue = found[2];
                           yearValue = found[1];
		        }
		     }

		     populateDates(fallbackDay, fallbackMonth, fallbackYear,
                      dayValue, monthValue, yearValue);

		     fallbackDay.addEventListener("change", dayListener);
		     fallbackMonth.addEventListener("change", monthListener);
		     fallbackYear.addEventListener("input", yearListener);
		  }

		  if (fallbackHour && fallbackMinute)
		  {
		     fallbackHour.value='';
		     fallbackMinute.value='';
		     fallbackSecond.value='';

		     fallbackHour.addEventListener("input", hourListener);
		     fallbackMinute.addEventListener("input", minuteListener);

		     if (fallbackSecond)
		     {
		        fallbackSecond.addEventListener("input", secondListener);

			if (dateTimeValue)
			{
	                   found = dateTimeValue.match(/(\d{2}):(\d{2}):(\d{2})/);

		           if (found)
		           {
                              fallbackHour.value = found[1];
                              fallbackMinute.value = found[2];
                              fallbackSecond.value = found[3];
		           }
			}
		     }
		     else if (dateTimeValue)
		     {
	                found = dateTimeValue.match(/^(\d{2}):(\d{2})/);

		        if (found)
		        {
                           fallbackHour.value = found[1];
                           fallbackMinute.value = found[2];
			}
		     }
		  }
	       }
	    }
         }
      }

      elems = document.getElementsByClassName("fallbackDateTimePicker");

      if (elems)
      {
         for (var i = 0; i < elems.length; i++)
         {
            elems[i].style.display='inline';
         }
      }
   }
}

function updateDateTime(field, fallbackDay, fallbackMonth, fallbackYear,
  fallbackHour = null, fallbackMinute = null, fallbackSecond = null)
{
   if (!fallbackDay)
   {
      fallbackDay = document.getElementById(field+"-day");
   }

   if (!fallbackMonth)
   {
      fallbackMonth = document.getElementById(field+"-month");
   }

   if (!fallbackYear)
   {
      fallbackYear = document.getElementById(field+"-year");
   }

   if (!fallbackHour)
   {
      fallbackHour = document.getElementById(field+"-hour");
   }

   if (!fallbackMinute)
   {
      fallbackMinute = document.getElementById(field+"-minute");

      if (!fallbackSecond)
      {
         fallbackSecond = document.getElementById(field+"-second");
      }
   }

   var dateStr = '';

   if (fallbackDay && fallbackMonth && fallbackYear)
   {
      dateStr = fallbackYear.value.padStart(4, '0')
         + '-' + fallbackMonth.value.padStart(2, '0')
         + '-' + fallbackDay.value.padStart(2, '0');
   }

   if (fallbackHour && fallbackMinute)
   {
      if (dateStr)
      {
         dateStr += 'T';
      }

      dateStr += fallbackHour.value.padStart(2, '0')
               + ':' + fallbackMinute.value.padStart(2, '0');

      if (fallbackSecond)
      {
         dateStr += ':' + fallbackSecond.value.padStart(2, '0');
      }
   }

   var nativeElem = document.getElementById(field);
   nativeElem.value = dateStr;
}

function dayListener()
{
  if (event.currentTarget.id)
  {
     const found = event.currentTarget.id.match(/^(.+)-day$/);

     if (found)
     {
        const field = found[1];

        var fallbackDay = event.currentTarget;
        var fallbackMonth = document.getElementById(field+"-month");
        var fallbackYear = document.getElementById(field+"-year");

	updateDateTime(field, fallbackDay, fallbackMonth, fallbackYear);
     }
  }
}

function monthListener()
{
  if (event.currentTarget.id)
  {
     const found = event.currentTarget.id.match(/^(.+)-month$/);

     if (found)
     {
        const field = found[1];

        var fallbackDay = document.getElementById(field+"-day");
        var fallbackMonth = event.currentTarget;
        var fallbackYear = document.getElementById(field+"-year");

        populateDates(fallbackDay, fallbackMonth, fallbackYear);

	updateDateTime(field, fallbackDay, fallbackMonth, fallbackYear);
     }
  }
}

function yearListener()
{
  if (event.currentTarget.id)
  {
     const found = event.currentTarget.id.match(/^(.+)-year$/);

     if (found)
     {
        const field = found[1];

        var fallbackDay = document.getElementById(field+"-day");
        var fallbackMonth = document.getElementById(field+"-month");
        var fallbackYear = event.currentTarget;

        if (fallbackYear.value)
        {
           fallbackYear.value = parseInt(fallbackYear.value);

           if (fallbackYear.value < 1000)
           {
              fallbackYear.value = fallbackYear.value.padStart(4, '0');
           }
        }

	if (fallbackMonth.value == 2)
	{
           populateDates(fallbackDay, fallbackMonth, fallbackYear);
	}

	updateDateTime(field, fallbackDay, fallbackMonth, fallbackYear);
     }
  }
}

function hourListener()
{
  if (event.currentTarget.id)
  {
     const found = event.currentTarget.id.match(/^(.+)-hour$/);

     if (found)
     {
        const field = found[1];

        var fallbackHour = event.currentTarget;
        var fallbackMinute = document.getElementById(field+"-minute");
        var fallbackSecond = document.getElementById(field+"-second");

	if (fallbackHour.value)
	{
	   if (fallbackHour.value < 10)
	   {
              fallbackHour.value = fallbackHour.value.padStart(2, '0');
	   }
           else
	   {
              fallbackHour.value = parseInt(fallbackHour.value);
	   }
	}

	updateDateTime(field, null, null, null, fallbackHour, fallbackMinute, fallbackSecond);
     }
  }
}

function minuteListener()
{
  if (event.currentTarget.id)
  {
     const found = event.currentTarget.id.match(/^(.+)-minute$/);

     if (found)
     {
        const field = found[1];

        var fallbackHour = document.getElementById(field+"-hour");
        var fallbackMinute = event.currentTarget;
        var fallbackSecond = document.getElementById(field+"-second");

	if (fallbackMinute.value)
        {
	   if (fallbackMinute.value < 10)
	   {
              fallbackMinute.value = fallbackMinute.value.padStart(2, '0');
	   }
           else
           {
              fallbackMinute.value = parseInt(fallbackMinute.value);
           }
        }

	updateDateTime(field, null, null, null, fallbackHour, fallbackMinute, fallbackSecond);
     }
  }
}

function secondListener()
{
  if (event.currentTarget.id)
  {
     const found = event.currentTarget.id.match(/^(.+)-second$/);

     if (found)
     {
        const field = found[1];

        var fallbackHour = document.getElementById(field+"-hour");
        var fallbackMinute = document.getElementById(field+"-minute");
        var fallbackSecond = event.currentTarget;

	if (fallbackSecond.value)
	{
	   if (fallbackSecond.value < 10)
	   {
              fallbackSecond.value = fallbackSecond.value.padStart(2, '0');
	   }
           else
           {
              fallbackSecond.value = parseInt(fallbackSecond.value);
           }
	}

	updateDateTime(field, null, null, null, fallbackHour, fallbackMinute, fallbackSecond);
     }
  }
}

function populateDates(dayElem, monthElem, yearElem, dayValue=null, monthValue=null, yearValue=null)
{
   if (!dayValue)
   {
      dayValue = dayElem.selectedIndex+1;
   }
   else
   {
      dayElem.selectedIndex = dayValue-1;
   }

   if (!monthValue)
   {
      monthValue = monthElem.selectedIndex-1;
   }
   else
   {
      monthElem.selectedIndex = monthValue+1;
   }

   if (!yearValue)
   {
      yearValue = yearElem.value;
   }
   else
   {
      yearElem.value = yearValue;
   }

   while (dayElem.firstChild)
   {
      dayElem.removeChild(dayElem.firstChild);
   }

   if (!monthValue)
   {
      monthValue = 1;
      monthElem.selectedIndex = 0;
   }

   var maxDay;

   if (monthValue == 1 || monthValue == 3 || monthValue == 5 || monthValue == 7 
    || monthValue == 8 || monthValue == 10 || monthValue == 12)
   {
      maxDay = 31;
   }
   else if (monthValue == 4 || monthValue == 6 || monthValue == 9 || monthValue == 11)
   {
      maxDay = 30;
   }
   else if (yearValue != '')
   {
      var isLeap = new Date(yearValue, 1, 29).getMonth() == 1;
      maxDay = (isLeap ? 29 : 28);
   }
   else
   {
      maxDay = 28;
   }

   if (dayValue > maxDay)
   {
      dayValue = maxDay;
   }

   for (var i = 1; i <= maxDay; i++)
   {
      var option = document.createElement('option');
      option.textContent = i;
      option.value = i;
      dayElem.appendChild(option);

      if (i == dayValue)
      {
         option.selected = true;
      }
   }
}

