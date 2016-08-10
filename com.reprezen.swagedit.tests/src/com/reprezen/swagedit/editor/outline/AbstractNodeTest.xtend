package com.reprezen.swagedit.editor.outline

import com.fasterxml.jackson.core.JsonPointer
import com.reprezen.swagedit.editor.SwaggerDocument
import com.reprezen.swagedit.json.JsonType
import com.reprezen.swagedit.model.Model
import org.junit.Test

import static org.junit.Assert.*

class AbstractNodeTest {

	@Test
	def void testParsing() {
		val text = '''
		  info:
		    title: b
		    version: 1.0.0
		'''

		val doc = new SwaggerDocument
		doc.set(text)

		val model = Model.parseYaml(text)		
		val root = model.root

		assertTrue(root.isObject)
		assertNull(root.parent)	
		assertNotNull(root.get("info"))

		val info = root.get("info")
		assertTrue(info.isObject)
		assertSame(root, info.parent)

		assertNotNull(info.get("title"))
		assertNotNull(info.get("version"))

		val title = info.get("title")
		assertFalse(title.object)
		assertFalse(title.array)
		assertEquals("b", title.asValue.getValue)

		val version = info.get("version")
		assertFalse(version.object)
		assertFalse(version.array)
		assertEquals("1.0.0", version.asValue.getValue)
		
		assertSame(info, model.find(":info"));
		assertSame(title, model.find(":info:title"));
		assertSame(version, model.find(":info:version"));
	}

	@Test
	def void testCreateFromSingleMapping() {
		val text = '''
		  foo: bar
		'''

		val model = Model.parseYaml(text)		
		val el = model.root

		assertEquals("foo: bar", el.text)
		assertEquals(0, el.elements().size)

		val doc = new SwaggerDocument
		doc.set(text)

		val position = el.getPosition(doc)
		// end of first line
		assertEquals(8, position.offset)
		assertEquals(0, position.length)

		// position is first line
		assertEquals(0, doc.getLineOfOffset(position.offset))
	}

	@Test
	def void testGetType_Enum() {
		val text = '''
		  swagger: 2.0
		'''

		val model = Model.parseYaml(text)		
		assertNotNull(model.root)

		val root = model.root

		assertEquals(JsonType.OBJECT, root.type)
		assertEquals("".ptr, root.pointer)
		assertNotNull(root.get("swagger"))
		assertEquals(JsonType.ENUM, root.get("swagger").type)
		assertEquals("swagger", root.get("swagger").property)
		assertEquals("/swagger".ptr, root.get("swagger").pointer)
	}

	@Test
	def void testGetType_Object() {
		val text = '''
		  info:
		    version: 1.0.0
		    title: hello
		'''

		val model = Model.parseYaml(text)		
		assertNotNull(model.root)

		val root = model.root

		assertEquals(JsonType.OBJECT, root.type)
		assertEquals("".ptr, root.pointer)

		assertNotNull(root.get("info"))

		val info = root.get("info")
		assertEquals(JsonType.OBJECT, info.type)
		assertEquals("info", info.property)
		assertEquals("/info".ptr, info.pointer)

		assertNotNull(info.get("version"))	
		assertNotNull(info.get("title"))

		val version = info.get("version")
		assertEquals(JsonType.STRING, version.type)
		assertEquals("version", version.property)
		assertEquals("/info/version".ptr, version.pointer)

		val title = info.get("title")
		assertEquals(JsonType.STRING, title.type)
		assertEquals("title", title.property)
		assertEquals("/info/title".ptr, title.pointer)
	}

	@Test
	def void testGetType_StringArray() {
		val text = '''
		  schemes:
		    - http
		    - https
		'''

		val model = Model.parseYaml(text)		
		assertNotNull(model.root)

		val root = model.root
		
		assertNotNull(root.get("schemes"))
		
		val schemes = root.get("schemes")
		assertTrue(schemes.isArray)
		assertEquals(JsonType.ARRAY, schemes.type)
		assertEquals("schemes", schemes.property)
		assertEquals("/schemes".ptr, schemes.pointer)
		assertEquals(2, schemes.elements.size)
		
		val first = schemes.elements.get(0)
		assertEquals("/schemes/0".ptr, first.pointer)
		
		val second = schemes.elements.get(1)
		assertEquals("/schemes/1".ptr, second.pointer)
		
		val doc = new SwaggerDocument
		doc.set(text)
		
		println(doc.asJson.at(schemes.pointer))
		println(doc.asJson.at(first.pointer))
		println(doc.asJson.at(second.pointer))
	}

	def ptr(String s) { JsonPointer.compile(s) }

	@Test
	def void testCreateFromArrayValues() {
		val text = '''
		  foo:
		    - hello
		    - world
		'''

		val model = Model.parseYaml(text)
		val el = model.root

		assertEquals("foo", el.text)
		assertEquals(2, el.elements.size)
		
		assertEquals("hello", el.elements.get(0).text)
		assertEquals("world", el.elements.get(1).text)

		val doc = new SwaggerDocument
		doc.set(text)

		val position = el.getPosition(doc)
		// after foo:
		assertEquals(4, position.offset)
		assertEquals(0, position.length)

		// should be first line
		assertEquals(0, doc.getLineOfOffset(position.offset))
	}

	@Test
	def void testCreateFromObjectValues() {
		val text = '''
		  foo: 
		    k1: hello
		    k2: world
		'''

		val model = Model.parseYaml(text)
		val el = model.root

		assertEquals("foo", el.text)
		assertEquals(2, el.elements.size)

		assertEquals("k1: hello", el.elements.get(0).text)
		assertEquals("k2: world", el.elements.get(1).text)

		val doc = new SwaggerDocument
		doc.set(text)

		val position = el.getPosition(doc)
		// after foo:
		assertEquals(4, position.offset)
		assertEquals(0, position.length)

		// should be first line
		assertEquals(0, doc.getLineOfOffset(position.offset))
	}
	
	@Test
	def void testGet() {
		val yaml = '''
		swagger: 2.0
		'''

		val model = Model.parseYaml(yaml)

		assertEquals("".ptr, model.getPath(0, 0))
		assertEquals("/swagger".ptr, model.getPath(0, 1))
		assertEquals("/swagger".ptr, model.getPath(0, 8))
	}

	def getPath(Model model, int i, int j) {
		model.getNode(i, j).pointer
	}

	@Test
	def void testGetPaths() {
		val yaml = '''
		info:
		  description: ""
		  version: "1.0.0"
		tags:
		  - foo: ""
		  - bar: ""
		'''

		val model = Model.parseYaml(yaml)

		assertEquals("/info".ptr, model.getPath(0, 1))
		assertEquals("/info/description".ptr, model.getPath(1, 13))
		assertEquals("/info/version".ptr, model.getPath(2, 9))
		assertEquals("/tags".ptr, model.getPath(3, 2))

		assertEquals("/tags/0/foo".ptr, model.getPath(4, 7))
		assertEquals("/tags/1/bar".ptr, model.getPath(5, 7))
	}

	@Test
	def void testGetPathOnEmptyLine() {
		val yaml = '''
		info:
		  description: ""
		  
		  version: "1.0.0"
		'''

		val model = Model.parseYaml(yaml)

		assertEquals("/info/description".ptr, model.getPath(1, 13))
		assertEquals("/info".ptr, model.getPath(2, 2))
		assertEquals("/info/version".ptr, model.getPath(3, 9))
	}

	@Test
	def void testGetPathOnEmptyLineAfter() {
		val yaml = '''
		info:
		  description: ""
		  version: "1.0.0"
		  
		'''

		val model = Model.parseYaml(yaml)

		assertEquals("/info/description".ptr, model.getPath(1, 14))
		assertEquals("/info/version".ptr, model.getPath(2, 9))
		assertEquals("/info".ptr, model.getPath(3, 2))
	}

	@Test
	def void testGetPathOnPaths() {
		val yaml = '''
		paths:
		  /:
		    get:
		      responses:
		        '200':
		'''

		val model = Model.parseYaml(yaml)

		assertEquals("/paths".ptr, model.getPath(0, 1));
		assertEquals("/paths/~1".ptr, model.getPath(1, 3));
		assertEquals("/paths/~1/get".ptr, model.getPath(2, 7));
		assertEquals("/paths/~1/get/responses".ptr, model.getPath(3, 14));
		assertEquals("/paths/~1/get/responses/200".ptr, model.getPath(4, 10));
	}

	@Test
	def void testGetPathOnPathsAfter() {
		val yaml = '''
		paths:
		  /:
		    
		'''

		val model = Model.parseYaml(yaml)

		assertEquals("/paths".ptr, model.getPath(0, 1));
		assertEquals("/paths".ptr, model.getPath(1, 1));
		assertEquals("/paths/~1".ptr, model.getPath(1, 3));
		assertEquals("/paths/~1".ptr, model.getPath(2, 3));
	}

	@Test
	def void testGetPath() {
		val yaml = '''
		paths:
		  /:
		    get:
		      responses:
		        '200':
		          description: OK
		    
		parameters:
		  foo:
		    name: foo
		'''
		
		val model = Model.parseYaml(yaml)
		
		assertEquals("/paths/~1".ptr, model.getPath(6, 3))
	}
}