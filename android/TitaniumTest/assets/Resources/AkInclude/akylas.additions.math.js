Math.arraySum = function(list) {
	var sum = 0;
	for (var i = 0, len = list.length; i < len; i++)
		sum += list[i];
	return sum;
};

Math.arrayMax = function(list) {
	var max = undefined;
	for (var i = 0, len = list.length; i < len; i++) {
		if (max === undefined || list[i] > max)
			max = list[i];
	}
	return max;
};

Math.arrayMean = function(list) {
	return list.length ? this.arraySum(list) / list.length : false;
};

Math.getClosestValues = function(a, x) {
	var lo = 0, hi = a.length - 1;
	while (hi - lo > 1) {
		var mid = Math.round((lo + hi) / 2);
		if (a[mid] <= x) {
			lo = mid;
		} else {
			hi = mid;
		}
	}
	if (a[lo] == x)
		hi = lo;
	return {
		lowIndex : lo,
		highIndex : hi
	};
}