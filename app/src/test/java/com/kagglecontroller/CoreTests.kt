package com.kagglecontroller

import com.kagglecontroller.core.network.KaggleException
import com.kagglecontroller.core.network.KaggleRpcClient
import com.kagglecontroller.domain.model.CellType
import com.kagglecontroller.domain.model.NotebookDoc
import com.kagglecontroller.domain.model.RunStatus
import com.kagglecontroller.feature.editor.EditorViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** No real Kaggle credentials are needed: everything runs against MockWebServer. */
class CoreTests {

    @Test fun statusParsing_handlesWireAndPythonStyleNames() {
        assertEquals(RunStatus.COMPLETE, RunStatus.parse("COMPLETE"))
        assertEquals(RunStatus.ERROR, RunStatus.parse("KernelWorkerStatus.ERROR"))
        assertEquals(RunStatus.UNKNOWN, RunStatus.parse("SOMETHING_NEW"))
        assertTrue(RunStatus.COMPLETE.terminal && !RunStatus.RUNNING.terminal)
    }

    @Test fun slugify_producesKaggleSafeSlugs() {
        assertEquals("my-first-notebook", EditorViewModel.slugify("My First Notebook!"))
        assertEquals("untitled-notebook", EditorViewModel.slugify("!!!"))
    }

    @Test fun ipynb_roundTripKeepsCells() {
        val text = NotebookDoc.serialize(
            listOf(
                com.kagglecontroller.domain.model.Cell(type = CellType.MARKDOWN, source = "# Title"),
                com.kagglecontroller.domain.model.Cell(type = CellType.CODE, source = "x = 1\nprint(x)"),
            ), "notebook", "python",
        )
        val cells = NotebookDoc.parse(text, "notebook")
        assertEquals(2, cells.size)
        assertEquals(CellType.MARKDOWN, cells[0].type)
        assertEquals("x = 1\nprint(x)", cells[1].source)
    }

    @Test fun script_isSingleCellAndRawText() {
        val cells = NotebookDoc.parse("print('hi')", "script")
        assertEquals(1, cells.size)
        assertEquals("print('hi')", NotebookDoc.serialize(cells, "script", "python"))
    }

    @Test fun rpc_sendsBearerTokenToConnectPath() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"status":"RUNNING"}"""))
            val client = KaggleRpcClient({ "KGAT_test" }, baseUrl = server.url("/").toString().trimEnd('/'))
            val res = client.write("kernels.KernelsApiService", "GetKernelSessionStatus", JsonObject(emptyMap()))
            val req = server.takeRequest()
            assertEquals("/v1/kernels.KernelsApiService/GetKernelSessionStatus", req.path)
            assertEquals("Bearer KGAT_test", req.getHeader("Authorization"))
            assertEquals("RUNNING", res["status"]!!.jsonPrimitive.content)
        }
    }

    @Test fun rpc_mapsHttpErrors() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "7"))
            val client = KaggleRpcClient({ "t" }, baseUrl = server.url("/").toString().trimEnd('/'))
            try { client.write("s", "m", JsonObject(emptyMap())); fail() } catch (e: KaggleException.Unauthorized) { }
            try { client.write("s", "m", JsonObject(emptyMap())); fail() } catch (e: KaggleException.RateLimited) { assertEquals(7, e.retryAfterSeconds) }
        }
    }

    @Test fun rpc_readRetriesOn429ThenSucceeds() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "0"))
            server.enqueue(MockResponse().setBody("""{"ok":true}"""))
            val client = KaggleRpcClient({ "t" }, baseUrl = server.url("/").toString().trimEnd('/'))
            client.read("s", "m", cacheTtlMs = 0)
            assertEquals(2, server.requestCount)
        }
    }

    @Test fun rpc_withoutTokenIsUnauthorizedAndNeverHitsNetwork() = runBlocking {
        MockWebServer().use { server ->
            val client = KaggleRpcClient({ null }, baseUrl = server.url("/").toString().trimEnd('/'))
            try { client.write("s", "m", JsonObject(emptyMap())); fail() } catch (e: KaggleException.Unauthorized) { }
            assertEquals(0, server.requestCount)
        }
    }
}
