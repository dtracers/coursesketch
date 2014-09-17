function swipeCheck() {
	if(!is_touch) {
		disable_menu_swiping();
	}
}

function placeMenu(isInstructor) {
	var menuElement = document.getElementById('menuBar');
	if(menuElement) {
		menuElement.innerHTML = getMenu(isInstructor);
		var loader = new DynamicFileLoader();
		console.log(swipeCheck);
		loader.loadFile('js/menu/sliding_menu.js', 'js', swipeCheck);
		loader.loadFile('css/menu/menu.css', 'css', false);
	}
}

function getMenu(isInstructor) {
	var home = isInstructor? "html/instructor/course_management.html" : "html/student/course_management.html";
	return	'<div id="menu">' +
			'	<h1>' +
			'		<a href="' + home + '" target ="wrapper" data-ajax="false"><img src="images/smallTitle.svg"></a>' +
			'	</h1>' +
			'	<ul id="menuList">' +
			'		<li><a href="' + home + '" target ="wrapper" data-ajax="false"' +
			'			class="contentLink">Home</a></li>' +
			((!isInstructor) ? // first part is student
			'		<li class="header"><h3>Classes I\'m In</h3></li>' +
			'		<li><a href="' + home + '" target ="wrapper" data-ajax="false">My Courses</a></li>' + 
			'		<li><a href="html/student/classSearch.html" target ="wrapper" data-ajax="false" class="contentLink">Add New Class</a></li>' +
			'		<li><a href="html/student/classOptions.html" target ="wrapper" data-ajax="false" class="contentLink">Remove Class</a></li>' +
			'		<li><a href="html/student/grades.html" target ="wrapper" data-ajax="false" class="contentLink">My Grades</a></li>' +
			'		<li><a href="html/student/roster.html" target ="wrapper" data-ajax="false" class="contentLink">Rosters</a></li>'  
			: // below is instructor
			'		<li class="header"><h3>Classes I\'m Teaching</h3></li>' +
			'		<li><a href="html/instructor/gradebook.html" target ="wrapper" data-ajax="false" class="contentLink">Grades</a></li>' +
			'		<li><a href="html/instructor/course_management.html" target ="wrapper" data-ajax="false" class="contentLink">Course Management</a></li>' +
			'		<li><a href="viewClassKeys.php" target ="wrapper" data-ajax="false" class="contentLink">View Class Keys</a></li>' +
			'		<li><a href="html/instructor/roster.html" target ="wrapper" data-ajax="false" class="contentLink">Roster</a></li>'
			) + // this is for both
			'		<li class="header"><h3>Account</h3></li>' +
			'		<li><a href="changePassword.php" target ="wrapper" data-ajax="false" class="contentLink">Account options</a></li>' +
			'		<li><a href="index.html" data-ajax="false" class="contentLink">Sign Out</a></li>' + //returns to login screen, but does not prevent user from re-accessing pages through the back button.
			'	</ul>' +
			'</div>';
}

function list_menu_classes(id) {
	var html = '';
	var class_list = document.getElementById('list_of_classes');
	var expanding_button = document.getElementById('expandable_arow_button' + id);
	if(class_list_showing) {
		class_list_showing = false;
		class_list.style.display = "none";
		expanding_button.src = "images/menu/triangle_right.png";
	} else {
		class_list_showing = true;
		var builder = new schoolItemBuilder();
		builder.setList(user_classes);
		builder.showAsSimpleList(true);
		html = builder.createSchoolList();
		class_list.style.display = "block";
		expanding_button.src = "images/menu/triangle_down.png";
		// ADD CLASSES HERE
	}
	class_list.innerHTML = html;

}

var class_list_showing = false;