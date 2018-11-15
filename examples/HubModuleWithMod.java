package net.floodlightcontroller.unipi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.util.HexString;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;

public class HubModuleWithMod implements IOFMessageListener, IFloodlightModule {
	
	protected IFloodlightProviderService floodlightProvider; // Reference to the provider

	@Override
	public String getName() {
		return HubModuleWithMod.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IFloodlightProviderService.class);
	    return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

			// Print the source MAC address
			Long sourceMACHash = Ethernet.toLong(eth.getSourceMACAddress().getBytes());
			System.out.printf("MAC Address: {%s} seen on switch: {%s}\n",
			HexString.toHexString(sourceMACHash),
			sw.getId());
			
			// Cast to Packet-In
			OFPacketIn pi = (OFPacketIn) msg;
			
			// Create a new rule to be added  
			OFFlowAdd.Builder fmb = sw.getOFFactory().buildFlowAdd();
	        fmb.setBufferId(pi.getBufferId())// Link the new rule to the received OF PKT IN
	        .setHardTimeout(20) // Set hard timeout
	        .setIdleTimeout(10) // Set soft timeout
	        .setPriority(32768) // Set priority
	        .setXid(pi.getXid());

	        // Create a new action to be execute 
	        OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
	        // Set as only action the packet to b flooded
	        actionBuilder.setPort(OFPort.FLOOD);
	        fmb.setActions(Collections.singletonList((OFAction) actionBuilder.build()));

	        // Since no matching rule is set this action will be executed for all the packets
	        
			sw.write(fmb.build());
			
			// Interrupt the chain
			return Command.STOP;

	}

}
