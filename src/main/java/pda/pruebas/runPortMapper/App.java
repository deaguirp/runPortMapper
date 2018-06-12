package pda.pruebas.runPortMapper;

import java.util.List;

import com.offbynull.portmapper.PortMapperFactory;
import com.offbynull.portmapper.gateway.Bus;
import com.offbynull.portmapper.gateway.Gateway;
import com.offbynull.portmapper.gateways.network.NetworkGateway;
import com.offbynull.portmapper.gateways.network.internalmessages.KillNetworkRequest;
import com.offbynull.portmapper.gateways.process.ProcessGateway;
import com.offbynull.portmapper.gateways.process.internalmessages.KillProcessRequest;
import com.offbynull.portmapper.mapper.MappedPort;
import com.offbynull.portmapper.mapper.PortMapper;
import com.offbynull.portmapper.mapper.PortType;

/**
 * Hello world!
 *
 */
public class App {
	private boolean shutdown=false;

	public static void main(String[] args) throws InterruptedException {
		new App().run();
	}
	
	private void run() throws InterruptedException{ 
		// Start gateways
		Gateway network = NetworkGateway.create();
		Gateway process = ProcessGateway.create();
		Bus networkBus = network.getBus();
		Bus processBus = process.getBus();

		// Discover port forwarding devices and take the first one found
		List<PortMapper> mappers = PortMapperFactory.discover(networkBus, processBus);
		PortMapper mapper = mappers.get(0);

		// Map internal port 12345 to some external port (55555 preferred)
		//
		// IMPORTANT NOTE: Many devices prevent you from mapping ports that are <= 1024
		// (both internal and external ports). Be mindful of this when choosing which
		// ports you want to map.
		MappedPort mappedPort = mapper.mapPort(PortType.TCP, 8080, 8080, 60);
		System.out.println("Port mapping added: " + mappedPort);

		// Refresh mapping half-way through the lifetime of the mapping (for example,
		// if the mapping is available for 40 seconds, refresh it every 20 seconds)
		long lt;
		while(!shutdown && (lt=mappedPort.getLifetime())>0) {
		    mappedPort = mapper.refreshPort(mappedPort, lt / 2L);
		    System.out.println("Port mapping refreshed: " + mappedPort);
		    Thread.sleep(mappedPort.getLifetime() * 1000L);
		}

		// Unmap port 12345
		mapper.unmapPort(mappedPort);

		// Stop gateways
		networkBus.send(new KillNetworkRequest());
		processBus.send(new KillProcessRequest()); // can kill this after discovery
	}
}
