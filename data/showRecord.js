$(document).ready(function() {
	var data;
	var list;
	var current;
	var myChart = echarts.init($("#main")[0]);

	$("#query").click(function () {
		var item1 = $("#date1")[0];
		var item2 = $("#date2")[0];
		var check = $("#check")[0];
		var url = "https://118.25.6.33:28443/kedamaListener/PlayerCountRecord"
//		var url = "https://127.0.0.1:28443/kedamaListener/PlayerCountRecord"
		var func;
		if(check.checked) {
			url += ("?" + "check=" + "now");
			func = 3;
		} else {
			if(item1.value == "" && item2.value == "") {
				url += ("?" + "list=" + "list")
				func = 2;
			} else {
				url += ("?" + "start=" + item1.value + "&end=" + item2.value);
				func = 1;
			}				
		}
		
		$.ajax({
			type: "get",
			url: url,
			async:false,
			dataType: "jsonp",
			jsonp: "jsoncallback",
			cache: false,
			beforeSend: function() {
				console.log(this.url);
			},
			success: function(resp, textStatus) {
				switch(func) {
				case 1:
					data = resp;
					console.log("#loaded data[" + data.length + "]");
					$("#loadInspector").html("#loaded data[" + data.length + "] from " + url);
					break;
				case 2:
					list = resp;
					var s = '<table border="1"><tbody>';
					s += '<tr><td>time</td><td>file</td></tr>';
					for(var i = 0; i < list.length; ++i)
						s += ('<tr><td>' + list[i].time + '</td><td>' + list[i].file + '</td></tr>');
					s += '</tbody></table>';
					$("#loadInspector").html(s);
					break;
				case 3:
					current = resp;
					var s = '<table style="border: dotted;"><tbody>';
					s += '<tr>';
					for(var i = 0; i < current.online.length; ++i)
						s += ('<td style="width:125px;">' + current.online[i] + '</td>');
					s += '</tr>';
					s += '</tbody></table>';
					$("#loadInspector").html(s);
					break;
				}
			},
			error: function(XMLHttpRequest, textStatus, errorThrown) {
				$("#loadInspector").html("<p>Error: " + textStatus + "</p>" + "<a href=" + '"' + url + '"' + " >download</a>");
			}
		});
	});
	
	var getRandomColor = function(){	
		for(var c = Math.random()*0xffffff; ((c >>> 0) & 0xff) < 0x80 && ((c >>> 8) & 0xff) < 0x80 && ((c >>> 16) & 0xff) < 0x80;c =  Math.random()*0xffffff);
		return '#' + (c << 0).toString(16);
	}
	
	var stdDay = new Date("2007-01-01T00:00:00.000+08:00")	//Monday
	var first;
	var myChart;
	
	function mergeData(period, data, search) {
		var data3 = [];
		var data2 = [];
		var maxOnline = 0;
		var lastContain = false;
		var includes = function(idata, search) {
			if(search == null)
				return lastContain = true;
			for(var j = 0; j < search.length; ++j)
				if(idata.online.includes(search[j]))
					return lastContain = true;
			if(lastContain) {
				lastContain = false;
				return true;
			} else {
				return false;
			}
			
		}
		if(data.length > 0)
			first = last = data[0].timestamp - (data[0].timestamp - stdDay.getTime()) % period;
		for(var i = 0, u = false; i < data.length; ++i) {
			if((u == true && lastContain == false ) || (data[i].continuous == false || data[i].timestamp - last > period) && data3.length > 0) {
				data2.push(data3);
				data3 = [];
				last = data[i].timestamp - (data[i].timestamp - stdDay.getTime()) % period;
			}
			if(u = includes(data[i], search)) {
				data3.push([(data[i].timestamp - stdDay.getTime()) % period, data[i].onlineNum, new Date(data[i].timestamp)]);
				if(data[i].onlineNum > maxOnline)
					maxOnline = data[i].onlineNum;
			}
		}
		if(data3.length > 0)
			data2.push(data3);
		return {
			dataset: data2,
			max: maxOnline
		}
	}
	
	
	
	var Week = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
	
	$("#draw").click(function draw() {
		try {
			data.length;
		} catch(e) {
			console.log(e);
			return;
		}
		var t1 = new Date();
		var period = $("#period")[0];
		var item4 = $("#search")[0];
		console.log(period.value);
		console.log(item4.value.split(','));
		var data2s = mergeData(period.value, data, item4.value == "" ? null : item4.value.split(','));
		var data2 = data2s.dataset;
		var maxY = ((data2s.max + 4) / 5 << 0) * 5;
		var option = {
			backgroundColor: '#21202D',
			title: {
				text: 'PlayerCountRecord',
				textStyle: {
					color: '#288312',
					fontSize: 20,
					align: 'center'
				}
			},
			tooltip: {
				trigger: 'axis',
				axisPointer: {
					animation: false,
					snap: true,
					type: 'line',
					lineStyle: {
						color: '#376df4',
						width: 2,
						opacity: 1
					}
				},
				formatter: function (params) {
					var d = params[0].data[2];
					var n = params[0].data[1];
					return Week[d.getDay()] + ' ' + echarts.format.formatTime('yyyy-MM-dd hh:mm:ss', d) + '<br />' + n;
				}
			},
			xAxis: null,
			yAxis: {
				min: 0,
				max: maxY,
				scale: true,
				axisLine: {
					lineStyle: {
						color: '#8392A5'
					}
				}
			},
			dataZoom: [
				{
					type: 'slider',
					labelFormatter: null,
					textStyle: {
						color: '#288312'
					}
				}
			],
			series: {
				type: 'line',
				data: [],
				lineStyle: {
					type: 'dotted',
					color: color,
					width: 1
				}
			}
        };

		switch(period.value) {
		case period.options[0].value:
			option.xAxis = {
				scale: true,
				axisLabel: {
					formatter: function(value) {
						return echarts.format.formatTime('yyyy-MM-dd hh:mm:ss', new Date(value + first));
					},
				},
				axisLine: {
					lineStyle: {
						color: '#8392A5'
					}
				}
			};
			break;
		case period.options[1].value:
			option.xAxis = {
				scale: true,
				axisLabel: {
					formatter: function(value) {
						return echarts.format.formatTime('hh:mm:ss', new Date(value + first));
					},
				},
				axisLine: {
					lineStyle: {
						color: '#8392A5'
					}
				},
				splitNumber: 12,
				minInterval: 1,
				maxInterval: 24 * 60 * 60 * 1000 / 12
			};
			break;
		case period.options[2].value:
			option.xAxis = {
				scale: true,
				axisLabel: {
					formatter: function(value) {
						var d = new Date(value + first);
						return Week[d.getDay()] + echarts.format.formatTime('hh:mm:ss', d);
					},
				},
				axisLine: {
					lineStyle: {
						color: '#8392A5'
					}
				},
				splitNumber: 7,
				minInterval: 1,
				maxInterval: 24 * 60 * 60 * 1000
			};
			break;
		}
		
		option.dataZoom[0].labelFormatter = option.xAxis.axisLabel.formatter;
		
		var isDiffPeroid = function(data2, i) {
			if(i == 0)
				return true;
			var a = data2[i][0][2].getTime() - data2[i][0][0];
			var b = data2[i-1][0][2].getTime() - data2[i-1][0][0];
			return a != b;
		}
		
		var color;
		if(data2.length > 0)
			option.series = [];
		for(var i = 0; i < data2.length; ++i) {
			if(period.value == Number.MAX_SAFE_INTEGER || isDiffPeroid(data2, i))
				color = getRandomColor();
			option.series.push({
				type: 'line',
				data: data2[i],
				lineStyle: {
					type: 'dotted',
					color: color,
					width: 1
				}
			});
		}
		console.log("prepared; delay=" + (new Date() - t1));
		if(data2.length > 0) {
			myChart.setOption(option, true);
			$("#showInspector").html();
		} else {
			$("#showInspector").html('<div style="background-color: rebeccapurple;" >no record</div>');
		}
	})
})
