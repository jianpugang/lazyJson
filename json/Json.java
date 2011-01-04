package json;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class Json implements JsonValue
{
	Map<String, JsonValue> data = new HashMap<String,JsonValue>();;
	
	boolean isList = false;

	public Json(String json)
	{
		ParsingJson parsing =new  ParsingJson(json);
		char[] brackets = parsing.bi.brackets;
		int lastIndex = brackets.length-1;
		if(brackets[0] == '[' && brackets[lastIndex]==']'){
			isList =true;
		}
		apply(parsing.rootValue);
	}
	
	private Json()
	{
	};
	/**
	 * make a new empty "Json" object 
	 * @param isList if true create a list style josn object,
	 * 	else map style 
	 * @return
	 */
	public static Json emptyJson(boolean isList)
	{
		Json j = new Json();
		if(isList){
			j.isList = isList;
		}
		return j;
	}
	private class ParsingJson
	{
		ParsingJson(String json)
		{
			jsonChar = json.toCharArray();
			bi = new BracketIndex(jsonChar);
			isList = (bi.brackets[0] == '[');
			
			if(bi.isLeafBegin(0)){
				int beginIndex = bi.indexs[0];
				int endIndex = bi.indexs[bi.indexs.length-1];
				if(isList){
					rootValue = fillArray(beginIndex+1,endIndex);
				}else{
					rootValue =fillObject(beginIndex+1,endIndex);
				}
				return;
			}
			
			int levels = bi.levels();
			if(levels > 5){
				levels = 5;
			}
			int[] lrIndexs = bi.level(levels);
			// construct leaf nodes
			// left index is key ,it's unique, can be ordered.
			Map<Integer,JsonValue> templeStore = spawnLeaf(lrIndexs);
						
			for(int l = levels-1; l>1; l--){
				int[] upLrIndexs = bi.level(l);
				spawnSub(templeStore, upLrIndexs);
			}
			if(isList){
				rootValue =fillUpArray(1, jsonChar.length-1, templeStore);
			}else{
				rootValue =fillUpObject(1, jsonChar.length-1, templeStore);
			}
		}
		final char[] jsonChar;
		BracketIndex bi ;
		Map<String,JsonValue> rootValue ;
		/**
		 * spawn first generation of json value,and they are leaf node.
		 * 
		 * @param lrIndexs index of '[' ']'
		 * 
		 * @return
		 */
		Map<Integer,JsonValue> spawnLeaf(int[] lrIndexs)
		{
			Map<Integer,JsonValue> templeStore = 
				new HashMap<Integer,JsonValue>();
			for( int i=0; i<lrIndexs.length; ){
				int left = lrIndexs[i++];
				int right = lrIndexs[i++];
				Map<String,JsonValue> jsonPart = null;
				// list json
				if(jsonChar[left] == '['){
					Json arrayJson = emptyJson(true);
					if(bi.isLeafBegin(left)){
						jsonPart = fillArray(left+1,	right);
					}else{
						jsonPart = fillDelayArray(left+1, right);
						/*
						here fillDelayArray() return the map have
						 an entry that key is ',',it hold delay JsonValue index
						 in jsonPart
						 
						 */
						JsonValue delayIndex = jsonPart.remove(",");
						String[] delayIndexs = delayIndex.toString().split(",");
						for(String index : delayIndexs){
							((JsonDelayLeaf) jsonPart.get(index))
								.parent = arrayJson;
						}
					}
										
					arrayJson.apply(jsonPart);
					// left is key of one json piece
					templeStore.put(left, arrayJson);
				}// object json
				else if(jsonChar[left] == '{'){
					Json objectJson = emptyJson(false);
					
					if(bi.isLeafBegin(left)){
						jsonPart = fillObject(left+1,right);
						
					}else{
						jsonPart = fillDelayObject(left+1, right);
						JsonValue delayIndexs = jsonPart.remove(":");
						String[] indexs = delayIndexs.toString().split(",");
						for(String index: indexs){
							((JsonDelayLeaf)jsonPart.get(index))
								.parent = objectJson;
						}
					}
					objectJson.apply(jsonPart);
					templeStore.put(left, objectJson);
				}
			}
			return templeStore;
		}
		/**
		 * just construct key's struct,this means this step used to get keys,
		 * values have been gegerated in previor step,it is first call after 
		 * "spawnLeaf",for every json part ,u need assign value which store in
		 * "templeStore" to key.
		 * @param templeStore 
		 * @param irIndexs 
		 * @param json
		 * @return
		 */
		Map<Integer,JsonValue> spawnSub(
				Map<Integer,JsonValue> templeStore,
				int[] irIndexs )
		{
			for( int i=0; i<irIndexs.length; ){
				int left = irIndexs[i++];
				int right = irIndexs[i++];
				// ????????
				if(bi.isLeafBegin(left)){
					//if current josn part is a leaf, allways it's not 
				}
				// list json
				if(jsonChar[left] == '['){
					Json arrayJson = emptyJson(true);
					// not fill Array ,is fillUpArray
					Map<String,JsonValue> jsonPart = 
						fillUpArray(left+1,	right,templeStore);
					arrayJson.apply(jsonPart);
					templeStore.put(left, arrayJson);
				}// object json
				else if(jsonChar[left] == '{'){
					Json objectJson = emptyJson(false);
					Map<String,JsonValue> jsonPart = 
						fillUpObject(left+1,	right,templeStore);
					objectJson.apply(jsonPart);
					templeStore.put(left, objectJson);
				}
			}
			return templeStore;
		}
		/**<pre>
		 * like fillDelayArray(),but when this method execute,inner have been
		 * created, fillDealyArray() treat inner as plain text,but this method
		 * put inner json object to key,key is inner left index ,the key 
		 * reference the inner object in "templeStore".
		 * </pre>
		 * @param json
		 */
		Map<String,JsonValue> fillUpArray(
				int begin,
				int end,
				Map<Integer,JsonValue> templeStore)
		{
			// locations.length == 4*n
			int[] locations = splitIgnorInner(begin,end,',');
			Map<String,JsonValue> jsonContent =
					new HashMap<String,JsonValue>();
			for(int l=0,i=0; l<locations.length;i++, l = l+4){
				JsonValue v = null;
				String stri = i+"";
				int offset = locations[l];
				int count = locations[l+3]-offset;
				// value is  inner 
				if(locations[l+1] > 0 && locations[l+2]>0){
					v =  templeStore.get(locations[l+1]);
				}else{
					String stringValue = new String(jsonChar,offset,count);
					stringValue = stringValue.trim();
					if(stringValue.length() == 0){
						continue;
					}
					v = getLeaf(stringValue);
				}
				jsonContent.put(stri, v);
			}
			return jsonContent;
		}
		/**
		 * not plain json array,but treat it as plain json, means delay parse.
		 * @param begin
		 * @param end
		 */
		Map<String,JsonValue> fillDelayArray(int begin,int end){
			// locations.length == 4*n
			int[] locations = splitIgnorInner(begin,end,',');
			Map<String,JsonValue> jsonPart = 
					new HashMap<String,JsonValue>();
			StringBuilder delayValueIndexs = new StringBuilder();
			for(int l=0,i=0; l<locations.length;i++,l= l+4){
				JsonValue v = null;
				String stri = i+"";
				int offset = locations[l];
				int count = locations[l+3]-offset;
				String stringValue = new String(jsonChar,offset,count);
				stringValue = stringValue.trim();
				if(stringValue.length() == 0){
					continue;
				}
				if(locations[l+1] > 0 && locations[l+2]>0){
					v  = getDelayedLeaf(stringValue);
					delayValueIndexs.append(stri).append(",");
				}else{
					v = getLeaf(stringValue);
				}
				jsonPart.put(stri, v);
			}
			// create a remprory string json leaf to delive delay index info
			// up forward.
			jsonPart.put(",", getLeaf("'"+delayValueIndexs.toString()+"'"));
			return jsonPart;
		}
		/**
		 * fill plain json array ,no inner struct
		 * @param begin beigen index in jsonChar
		 * @param end  end index in josnchar
		 * @param propIsDelayed propertities delay parsing.
		 */
		Map<String,JsonValue> fillArray(int begin,int end)
		{
			// locations.length == 2*n
			Map<String,JsonValue> jsonPart = 
				new HashMap<String,JsonValue>();
			if(end == begin){
				return jsonPart;
			}
			int[] locations = split(begin,end,',');
			for(int l=0,i=0 ;l < locations.length; i++,l=l+2){
				JsonValue v = null;
				String stri = i+"";
				int offset = locations[l];
				int count = locations[l+1]-offset;
				String value = new String(jsonChar,offset,count);
				// remove blank i mean trim
				value = value.trim();
				if(value.length() == 0){
					continue;
				}
				v = getLeaf(value);
				jsonPart.put(stri, v);
			}
			return jsonPart;
		}
		/**
		 * like fillDelayObject(), assign inner to json object
		 * @param begin
		 * @param end
		 */
		Map<String,JsonValue> fillUpObject(
				int begin,
				int end,
				Map<Integer,JsonValue> templeStore)
		{
			Map<String,JsonValue> jsonContent =
					new HashMap<String,JsonValue>();
			if(end == begin){
				return jsonContent;
			}
			int[] locations = splitIgnorInner(begin, end, ',');
			for(int l=0; l<locations.length;l=l+4){
				int entryBeginIndex = locations[l];
				int entryEndIndex = locations[l+3];
				
				int[] pair = splitIgnorInner(
						entryBeginIndex,
						entryEndIndex,
						':');

				int keyOffset  = pair[0];
				int keyCount = pair[3] - keyOffset;
				String key = new String(jsonChar,keyOffset, keyCount);
				key  = key.trim();		
				if(key.length() == 0){
					continue;
				}
				JsonValue value = null;
				// value is inner
				if(pair[5] > 0 && pair[6] > 0){
					value = templeStore.get(pair[5]);
				}else{
					int valueOffset = pair[4];
					int valueCount = pair[7]-valueOffset;
					String valueString = new String(jsonChar,valueOffset,valueCount);
					valueString = valueString.trim();
					value = getLeaf(valueString);
				}
				jsonContent.put(key,value );
			}
			return jsonContent;
		}
		/**<pre>
		 * about pair:
		 * pair[0] key begin index
		 * pair[1] key's inner begin index
		 * pair[2] key's index end index
		 * pair[3] key end index
		 * pair[4] value begin index
		 * pair[5] value's inner begin index
		 * pair[6] value's inner end index
		 * pair[7] value end index 
		 * 
		 * begin , end is subString like params
		 * </pre>
		 * 
		 * @param begin 
		 * @param end
		 */
		Map<String,JsonValue> fillDelayObject(int begin,int end)
		{
			int[] locations = splitIgnorInner(begin, end, ',');
			Map<String,JsonValue> jsonContent = 
					new HashMap<String,JsonValue>();
			StringBuilder delayIndex = new StringBuilder();
			for(int l=0; l<locations.length;l = l+4){
				int entryBeginIndex = locations[l];
				int entryEndIndex = locations[l+3];
				int[] pair = splitIgnorInner(entryBeginIndex,entryEndIndex,':');
				
				LeafJsonValue leaf = null;
				int keyOffset  = pair[0];
				int keyCount = pair[3] - keyOffset;
				String key = new String(jsonChar,keyOffset, keyCount);
				key = key.trim();
				if(key.length() ==0){
					continue;
				}
				int valueOffset = pair[4];
				int valueCount = pair[7]-valueOffset;
				String value = new String(jsonChar,valueOffset,valueCount);
				value = value.trim();
				if(pair[5] > 0 && pair[6] > 0){
					leaf = getDelayedLeaf( value);
					delayIndex.append(key).append(",");
				}else{
					leaf = getLeaf(	value);
				}
				jsonContent.put(key,leaf );
			}
			jsonContent.put(":", getLeaf("'"+delayIndex.toString()+"'"));
			return jsonContent;
		}
		/**<pre>
		 * plain object
		 * about pair:
		 * pair[0] key begin index
		 * pair[1] key end index
		 * pair[2] value begin index
		 * pair[3] value end index
		 * 
		 * </pre>
		 * @param begin
		 * @param end
		 * @param 
		 */
		Map<String,JsonValue> fillObject(int begin,int end)
		{
			Map<String,JsonValue> jsonContent = 
				new HashMap<String,JsonValue>();
			if(end == begin){
				return jsonContent;
			}
			int[] locations = split(begin,end,',');
			for(int l=0; l<locations.length;){
				// 4 index 
				int[] pair = split(locations[l++],locations[l++], ':');
				LeafJsonValue leaf = null;
				int keyOffset  = pair[0];
				int keyCount = pair[1] - keyOffset;
				String key = new String(jsonChar,keyOffset, keyCount);
				key = key.trim();
				if(key.length()==0){
					continue;
				}
				int valueOffset = pair[2];
				int valueCount = pair[3]-valueOffset;
				String value = new String(jsonChar,valueOffset,valueCount);
				value = value.trim();
				leaf = getLeaf(	 value);
				jsonContent.put(key,leaf );
			}
			return jsonContent;
		}
		/**
		 *  ignor inner '[]','{}'
		 * @param begin
		 * @param end
		 * @param saparator
		 * @return
		 * if [] :
		 */
		private int[] splitIgnorInner(int begin,int end,char saparator)
		{
			int start = begin;
			int realLeng = 0;
			int[] rawLocation = new int[(end-begin)*4];
			boolean commaAfterPlain = true;
			for(int i=begin; i<end; i++){
				char c = jsonChar[i];
				
				if(c=='\''){
					boolean notRightQuote = true;
					while(notRightQuote){
						notRightQuote = (jsonChar[++i]  != '\'');
					}
				}else if(c=='"'){
					boolean notRightQuote = true;
					while(notRightQuote){
						notRightQuote = (jsonChar[++i]  != '"');
					}
				}else if( (bi.reverseIndexs[i]>0)/*indexIsBracket*/){
					int matchedRightIndex = bi.matchedRightIndex(i);
					/* split begin index
					inner's end index same as 
					[[],{}]
					{a:[],b:{}}
					 when u find a inner ,u really get :
					 inner begin ,inner end,outter end
					 (inner end == outter end)
					**/
					rawLocation[realLeng++] = start;
					rawLocation[realLeng++] = i;
					rawLocation[realLeng++] =matchedRightIndex+1;
					rawLocation[realLeng++] = matchedRightIndex+1;
					start = matchedRightIndex+1;
					i = matchedRightIndex;
					commaAfterPlain = false;
				}else if( c == saparator){
					if (commaAfterPlain){
						rawLocation[realLeng++] = start;
						rawLocation[realLeng++] = -1;
						rawLocation[realLeng++] = -1;
						rawLocation[realLeng++] = i;
					}
					//reset  two flag
					commaAfterPlain = true;
				
					// not special symbol(bracket),just process saparator
					start = i+1;
				}
			}
			if( start < end){
				if(commaAfterPlain)	{
					rawLocation[realLeng++] = start;
					rawLocation[realLeng++] = -1;
					rawLocation[realLeng++] = -1;
					rawLocation[realLeng++] = end;
				}
			}
			int[] realLocation = new int[realLeng];
			System.arraycopy(rawLocation, 0, realLocation, 0, realLeng);
			return realLocation;
		}
		/**
		 * 
		 * @param begin index not include {} []
		 * @param end
		 * @param saparator
		 * @return
		 */
		private  int[] split(int begin,int end,char saparator)
		{
			int leng = end -begin;
			int[] rawLocation = new int[leng+leng];
			int realLeng = 0;
			int start = begin;
			for(int i=begin ; i< end; i++){
				char c = jsonChar[i];
				if(c=='\''){
					boolean notRightQuote = true;
					while(notRightQuote){
						notRightQuote = (jsonChar[++i]  != '\'');
					}
				}else if(c=='"'){
					boolean notRightQuote = true;
					while(notRightQuote){
						notRightQuote = (jsonChar[++i]  != '"');
					}
				}else if( c == saparator){
					if(start < i){
						rawLocation[realLeng++] = start;
						rawLocation[realLeng++] = i;
					}
					start = i+1;
				}
			}
			if(start < end ){
				rawLocation[realLeng++] = start;
				rawLocation[realLeng++] = end;
			}
			int[] realLocation = new int[realLeng];
			System.arraycopy(rawLocation, 0, realLocation, 0, realLeng);
			return realLocation;
		}

	}
	
		
	private class BracketIndex
	{
		public BracketIndex(char[] chars)
		{
			int length = chars.length;
			char[] rawBrackets = new char[length];
			int[] rawIndexs = new int[length];
			int[] rawReverseIndexs = new int[length];
			int realLeng = 0;
			for(int i=0; i<length;i++){
				char c = chars[i];
				if(c == '\'' ){
					boolean notRightQuote = true;
					while(notRightQuote){
						rawReverseIndexs[i] = -1;
						notRightQuote = chars[++i] != '\'' ;
					}
				}else if(c == '"'){
					boolean notRightQuote = true;
					while(notRightQuote){
						rawReverseIndexs[i] = -1;
						notRightQuote = chars[++i] !='"' ;
					}
				}
				else if(c=='{' || c=='}' || c == '[' || c==']'){
					rawBrackets[realLeng] = c;
					rawIndexs[realLeng] =i;
					rawReverseIndexs[i] = realLeng;
					realLeng ++;
				}else if( c ==' ' || c == '\n' || c == '\r' || c =='\t'){
						
					rawReverseIndexs[i] = -1;
				}
				
			}
			brackets = new char[realLeng];
			indexs = new int[realLeng];
			reverseIndexs = rawReverseIndexs;
			
			System.arraycopy(rawBrackets,0, brackets, 0, realLeng);
			System.arraycopy(rawIndexs,0, indexs, 0, realLeng);
		}
		
		public char[] brackets;
		public int[] indexs;
		// indexs values is ordered,to find index of one value in "indexs",
		// use binary search is good,but use reverseIndexs to store these
		// indexs is better. replace "BS".
		public int[] reverseIndexs;

		public int levels()
		{
			int levels = 0;
			int n = 0;
			for( int i=0; i<brackets.length; i++){
				char c = brackets[i];
				if( c == '[' || c == '{'){
					n++;
					if(n>levels){
						levels = n;
					}
				}else if(c == ']' || c == '}'){
					n--;
				}
			}
			return levels;
		}
		/**
		 * 
		 * @param level 
		 * @return
		 */
		public int[] level(int level)
		{
			if(level == 0){return new int[]{0};}
			int i = 0;
			int l = 0;
			int n = 0;
			for(; l< level && i< brackets.length; i++){
				char c = brackets[i];
				if( c == '[' || c == '{'){
					n++;
					if(n>l){ l = n;}
				}else if(c ==']' || c == '}'){
					n--;
				}
			}
			if( l < level){
				return null;
			}
			int leng = 0;
			int nextBrotherBeginIndex =i-1;
			//brackets.length/2 bigest probably length 
			int[] levelIndexs = new int[brackets.length];
			while(nextBrotherBeginIndex > 0){
				levelIndexs[leng++] = indexs[nextBrotherBeginIndex];
				int levelBrotherEndIndex = matchedRightIndex0(nextBrotherBeginIndex);
				levelIndexs[leng++] = indexs[levelBrotherEndIndex];
				nextBrotherBeginIndex = nextBegin(levelBrotherEndIndex);
			}
			int[] fixedLevelIndex = new int[leng];
			if(leng == brackets.length){
				return levelIndexs;
			}
			System.arraycopy(levelIndexs, 0, fixedLevelIndex, 0, leng);
			return fixedLevelIndex;
		}
		/**
		 * get next right brother begin index 
		 * @param preEndIndex
		 * @return
		 */
		private int nextBegin(int preEndIndex){
			//()
			if(preEndIndex +1 == brackets.length){
				return -1;
			}
			if(	brackets[preEndIndex+1]==']' ||
			    brackets[preEndIndex+1]=='}')
			{
				return -1;
			}
			return preEndIndex+1;
		}
		/**
		 * get next closest right brother
		 * @param leftIndex index of position closest to left brackets
		 * @return
		 */
		private int next(int leftIndex)
		{
			int i = leftIndex;
			while( brackets[i]!='{' &&
					brackets[i] !='[')
			{
				i--;
			}
			int matchedRightIndex = matchedRightIndex0(i);
			return nextBegin(matchedRightIndex);
		}
		/**
		 * 
		 * @param leftIndex index of '{' or '[' in this.brackets
		 * @return
		 */
		private int matchedRightIndex0(int leftIndex)
		{
			// inner []
			int inner0 = 0;
			// inner  {}
			int inner1 = 0;
			int i = leftIndex;
			char left = brackets[i];
			char right ='\0';
			if( left == '['){
				right = ']';
			}else if( left == '{'){
				right = '}';
			}
			while(!(
					brackets[++i]==right 
					&& inner0 == 0 
					&& inner1 ==0)){
					char c = brackets[i];
					if( c == '['){
						inner0++;
					}else if(c == '{'){
						inner1++;
					}else if(c == ']'){
						inner0--;
					}else if(c == '}'){
						inner1--;
					}
			}
			if( i <brackets.length){
				return i;
			}
			return -1;
		}
		public int matchedRightIndex(int leftJsonIndex)
		{
			int leftIndex = reverseIndexs[leftJsonIndex];
			int rightIndex =  matchedRightIndex0(leftIndex);
			return indexs[rightIndex];
		}
		public int matchedLeftIndex(int rightJsonIndex)
		{
			int rightIndex = reverseIndexs[rightJsonIndex];
			int leftIndex = matchedLeftIndex0(rightIndex);
			return indexs[leftIndex];
		}
		/**
		 * 
		 * @param rightIndex }] index in json string
		 * @return
		 */
		private int matchedLeftIndex0(int rightIndex)
		{
			// inner []
			int inner0 = 0;
			// inner  {}
			int inner1 = 0;
			int i = rightIndex;
			char right = brackets[i];
			char left ='\0';
			if( right == ']'){
				left = '[';
			}else if( right == '}'){
				left = '{';
			}
			while(!(
					brackets[--i]==left 
					&& inner0 == 0 
					&& inner1 ==0)){
					char c = brackets[i];
					if( c == ']'){
						inner0++;
					}else if(c == '}'){
						inner1++;
					}else if(c == '['){
						inner0--;
					}else if(c == '{'){
						inner1--;
					}
			}
			if( i > -1){
				return i;
			}
			return -1;
		}
		public boolean isLeafBegin(int leftIndex)
		{
			int i = reverseIndexs[leftIndex];
			char left = brackets[i];
			if(left=='['){
				return brackets[i+1]==']';
			}else if(left=='{'){
				return brackets[i+1]=='}';
			}
			return false;
		}
	}
	/**
	 * 
	 * @param key
	 * @param value json value that not parsed currently
	 * @return
	 */
	private LeafJsonValue getDelayedLeaf(String value)
	{
		return new JsonDelayLeaf(value);
	}
	private  LeafJsonValue getLeaf(String value)
	{
		// is string
		if((value.startsWith("\"") && value.endsWith("\""))
				||
			(value.startsWith("'") && value.endsWith("'")))
		{
			return new JsonStringLeaf(value);
		}// is bool
		else if("true".equals(value) )
		{
			return new JsonBoolLeaf(true);
		}// is bool
		else if( "false".equals(value))
		{
			return new JsonBoolLeaf( false);
		}// is number
		else {
			return new JsonNumberLeaf(Double.parseDouble(value));
		}
	}

	public String toJson()
	{
		if(isList){
			return toJson4list();
		}
		return toJson4map();
	}
	/**
	 * toJson for list style json
	 * @return
	 */
	private String toJson4list()
	{
		StringBuilder json = new StringBuilder();
		json.append("[");
		int size  = data.size();
		if(size > 0){
			
			for(int i =0; i<size; i++){
				JsonValue value = data.get(i+"");
				if(value == null){
					continue;
				}
				json.append(value.toJson()).append(",");
			}
			json.deleteCharAt(json.length()-1);
		}
		json.append("]");
		return json.toString();
	}
	/**
	 * toJson for map style json
	 * @return
	 */
	private String  toJson4map()
	{
		StringBuilder json = new StringBuilder();
		json.append("{");
		
		if(data.size() > 0){
			for(Entry<String,JsonValue> entry : data.entrySet()){
				String key = entry.getKey();
				JsonValue value = entry.getValue();
				json.append(key).append(":")
				.append(value.toJson()).append(",");
			}
			json.deleteCharAt(json.length()-1);
		}
		json.append("}");
		return json.toString();
	}
	/**
	 * read a json property
	 */
	public JsonValue $(String k)
	{
		return data.get(k);
	}
	public JsonValue  $$(String path)
	{
		String[] dirs = path.split("#");
		JsonValue currentJson = this;;
		for(String dir : dirs){
			currentJson = currentJson.$(dir);
		}
		return currentJson;
	}
	public double double$$(String path)
	{
		return ((LeafJsonValue)$$(path)).getNumber();
	}
	public String string$$(String path)
	{
		return ((LeafJsonValue)$$(path)).getString();
	}
	public boolean bool$$(String path,boolean bool){
		return ((LeafJsonValue)$$(path)).getBool();
	}
	public boolean isLeaf()
	{
		return false;
	}
	/**
	 * put a property
	 */
	public JsonValue w(String k, JsonValue v)
	{
		if(this.equals(v)){
			throw new RuntimeException("bite tail!!" );
		}
		return data.put(k, v);
	}
	/**
	 * 
	 * @param path  a#b#c#d like directory links
	 * @param v
	 * @return
	 */
	public JsonValue ww(String path,JsonValue v)
	{
		String[] dirs = path.split("#");
		JsonValue currentJson = this;;
		for(int i=0; i<dirs.length-1; i++){
			currentJson = currentJson.$(dirs[i]);
		}
		JsonValue old =  currentJson.w(dirs[dirs.length-1], v);
		return old;
	}
	public JsonValue ww(String path,double number)
	{
		JsonNumberLeaf leaf = new JsonNumberLeaf(number);
		return ww(path,leaf);
	}
	public JsonValue ww(String path,String string)
	{
		JsonStringLeaf leaf = new JsonStringLeaf(string);
		return ww(path,leaf);
	}
	public JsonValue ww(String path,boolean bool){
		JsonBoolLeaf leaf = new JsonBoolLeaf(bool);
		return ww(path,leaf);
	}
	/**
	 * add some proterties to json
	 * @param jsonPart
	 */
	public void apply(Map<String,JsonValue> jsonPart)
	{
		data.putAll(jsonPart);
	}
	/**
	 * othe toString() output like json, this is real json!
	 */
	public String toString()
	{
		return toJson();
	}
	public boolean equals(Object other)
	{
		if(!(other instanceof Json)){
			return false;
		}
		Json otherJson = (Json) other;
		return this.data.equals(otherJson.data) ;
	}
	public int hashCode()
	{
		return data.hashCode();
	}
	public Collection<JsonValue> values()
	{
		return data.values();
	}
	public Collection<String> keys(){
		return data.keySet();
	}
	

}
